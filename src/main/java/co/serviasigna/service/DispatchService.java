package co.serviasigna.service;

import co.serviasigna.domain.*;
import co.serviasigna.engine.*;
import co.serviasigna.service.view.*;
import co.serviasigna.users.UserAccount;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Casos de uso de la operación: todo pasa por aquí (la central, los domiciliarios y el planificador).
 *
 * Cómo guarda la información: la operación en curso vive en memoria (servicios abiertos, rutas, ofertas pendientes)
 * para que el motor sea rápido, y CADA cambio se escribe en Oracle en el mismo momento (servicio, parada, historial,
 * oferta, evidencia, turno...). Al reiniciar, cargar() reconstruye todo desde la base de datos.
 *
 * Es "synchronized" para que dos acciones no se pisen (RNF-05): por ejemplo, si dos domiciliarios aceptan la misma
 * oferta a la vez, el primero gana y el segundo recibe "otro domiciliario ya lo tomó" (RN-02).
 */
@Service
public class DispatchService {
    private static final Logger log = LoggerFactory.getLogger(DispatchService.class);
    static final ZoneId BOGOTA = ZoneId.of("America/Bogota");                       // RN-17
    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9+() -]{7,20}$");
    private static final int MAX_IMAGE = 900_000;                                  // foto/firma en base64 (~650 KB)
    private static final Set<DeliveryStatus> OPEN_STATUSES = EnumSet.of(DeliveryStatus.CREATED, DeliveryStatus.SCHEDULED,
            DeliveryStatus.QUEUED, DeliveryStatus.OFFERED, DeliveryStatus.ASSIGNED, DeliveryStatus.IN_PROGRESS);

    private final DataStore db;
    private final AssignmentEngine engine;
    private final Settings settings;
    private final OperatingArea area;
    private final Clock clock;
    private final Routing routing;
    private final Notifier notifier;
    private final SecureRandom random = new SecureRandom();

    // ---- Estado de la operación en memoria ----
    private final Map<Long, Delivery> deliveries = new LinkedHashMap<>();     // abiertos + cerrados de las últimas 24 h
    private final Map<Long, Courier> couriers = new LinkedHashMap<>();
    private final Map<Long, Offer> offers = new LinkedHashMap<>();         // solo las pendientes
    private final Map<Long, double[]> offerInfo = new HashMap<>();          // offerId -> {kmExtra, etaDropoff}
    private final Map<Long, Set<Long>> rejectedBy = new HashMap<>();         // deliveryId -> domiciliarios que lo rechazaron
    private final Map<Long, Shift> shifts = new HashMap<>();                 // courierId -> turno abierto
    private final Map<Long, Vehicle> vehicleOf = new HashMap<>();          // courierId -> vehículo activo
    private final Map<Long, AccountStatus> accountOf = new HashMap<>();
    private final Map<Long, Integer> unreadMessages = new HashMap<>();      // mensajes del domiciliario que la central no ha leído
    private Distances lastMatrix;
    private String engineFailure;
    private int openIncidents;
    private double lastReorder, lastEligibility, lastCleanup;
    private Rate rate;
    private CentralView cachedView;
    private long version, cachedVersion = -1;

    public DispatchService(DataStore db, AssignmentEngine engine, Settings settings, OperatingArea area, Clock clock,
                           Routing routing, Notifier notifier) {
        this.db = db; this.engine = engine; this.settings = settings; this.area = area; this.clock = clock;
        this.routing = routing; this.notifier = notifier;
        this.lastMatrix = null;
    }

    // =====================================================================================
    // Arranque: reconstruir la operación desde Oracle
    // =====================================================================================

    public synchronized void load() {
        double now = clock.now();
        deliveries.clear(); couriers.clear(); offers.clear(); shifts.clear(); vehicleOf.clear();
        for (Courier d : db.couriers().findAll()) {
            if (d.getPosition() == null) d.setPosition(area.getCenter());
            d.signal(now);
            couriers.put(d.getId(), d);
        }
        // Servicios abiertos + los cerrados de las últimas 24 horas (para el resumen del día)
        List<Delivery> list = new ArrayList<>(db.deliveries().findByStatusIn(OPEN_STATUSES));
        for (Delivery s : db.deliveries().findByCreatedBetweenOrderByIdDesc(now - 1440, now + 1))
            if (!s.isOpen()) list.add(s);
        list.sort(Comparator.comparing(Delivery::getId));
        for (Delivery s : list) deliveries.put(s.getId(), s);

        // Paradas
        Map<Long, List<DeliveryStop>> byDelivery = new HashMap<>();
        if (!deliveries.isEmpty())
            for (DeliveryStop p : db.stops().findByDeliveryIdIn(deliveries.keySet()))
                byDelivery.computeIfAbsent(p.getDeliveryId(), k -> new ArrayList<>()).add(p);
        for (Delivery s : deliveries.values()) {
            DeliveryStop r = null, e = null;
            for (DeliveryStop p : byDelivery.getOrDefault(s.getId(), List.of())) {
                if (p.getType() == StopType.PICKUP) r = p; else e = p;
            }
            if (r == null) r = db.stops().save(new DeliveryStop(s.getId(), StopType.PICKUP));
            if (e == null) e = db.stops().save(new DeliveryStop(s.getId(), StopType.DROPOFF));
            s.setStops(r, e);
        }

        // Ofertas que quedaron pendientes al apagar: vencen y el servicio vuelve a la cola
        for (Offer o : db.offers().findByStatus(OfferStatus.PENDING)) {
            o.close(OfferStatus.EXPIRED, now, "El sistema se reinició");
            db.offers().save(o);
        }
        for (Delivery s : deliveries.values())
            if (s.getStatus() == DeliveryStatus.OFFERED) change(s, DeliveryStatus.QUEUED, "sistema", "SISTEMA", "El sistema se reinició", now);

        // Rutas: cada domiciliario recupera sus paradas pendientes en el orden guardado
        Map<Long, List<DeliveryStop>> byCourier = new HashMap<>();
        for (Delivery s : deliveries.values()) {
            for (DeliveryStop p : List.of(s.getPickupStop(), s.getDropoffStop())) {
                if (p.getCourierId() == null || p.getStatus() == StopStatus.COMPLETED || p.getStatus() == StopStatus.FAILED) continue;
                if (p.getType() == StopType.PICKUP && s.getPickedUp() != null) continue;
                boolean current = s.getStatus().withCourier() || (s.isReturnTrip() && p.getType() == StopType.DROPOFF);
                if (current) byCourier.computeIfAbsent(p.getCourierId(), k -> new ArrayList<>()).add(p);
            }
        }
        for (Map.Entry<Long, List<DeliveryStop>> e : byCourier.entrySet()) {
            Courier d = couriers.get(e.getKey());
            if (d == null) continue;
            List<DeliveryStop> ps = e.getValue();
            ps.sort(Comparator.comparing(p -> p.getSequence() == null ? 999 : p.getSequence()));
            List<Stop> route = new ArrayList<>();
            for (DeliveryStop p : ps) route.add(new Stop(deliveries.get(p.getDeliveryId()), p.getType()));
            d.setRoute(route);
        }

        for (Shift t : db.shifts().findByStatus("ABIERTO")) shifts.put(t.getCourierId(), t);
        for (Courier d : couriers.values()) refreshEligibility(d);
        rate = db.rates().findFirstByActiveTrueOrderByIdDesc().orElse(null);
        openIncidents = db.incidents().findByStatusNotOrderByIdDesc(IncidentStatus.CLOSED).size();
        for (ChatMessage m : db.messages().findByReadFalse())
            if (!m.isFromCentral()) unreadMessages.merge(m.getCourierId(), 1, Integer::sum);
        Environment e = environment(routing.estimated());
        for (Courier d : couriers.values()) if (!d.getRoute().isEmpty()) updateRoute(d, e);
        lastEligibility = now;
        log.info("Operación cargada: {} servicios, {} domiciliarios, {} rutas activas", deliveries.size(), couriers.size(), byCourier.size());
        changed();
    }

    // =====================================================================================
    // Central: servicios
    // =====================================================================================

    public synchronized Delivery createDelivery(DeliveryData d, String username) {
        double now = clock.now();
        validatePoint(d.pickup(), "recogida"); validatePoint(d.dropoff(), "entrega");
        if (d.pickup().distanceM(d.dropoff()) < 50) throw new IllegalArgumentException("La recogida y la entrega no pueden ser el mismo lugar");

        double deadlineMin = d.deadlineMin() == null ? settings.number("servicio.limite-defecto-min") : d.deadlineMin();
        double readyMin = d.readyMin() == null ? 0 : d.readyMin();
        double maxPickupMin = d.maxPickupMin() == null ? settings.maxPickupMin() : d.maxPickupMin();
        if (deadlineMin < 5) throw new IllegalArgumentException("El tiempo para entregar debe ser de mínimo 5 minutos");
        if (deadlineMin > 24 * 60) throw new IllegalArgumentException("El tiempo para entregar no puede pasar de 24 horas");
        if (readyMin < 0) throw new IllegalArgumentException("El tiempo en que estará listo el pedido no puede ser negativo");
        if (readyMin >= deadlineMin) throw new IllegalArgumentException("El pedido debe estar listo antes del límite de entrega");
        if (maxPickupMin < 1) throw new IllegalArgumentException("El tiempo máximo para recoger debe ser de al menos 1 minuto");

        Double scheduled = d.scheduledFor();
        if (scheduled != null) {
            if (scheduled < now + 1) throw new IllegalArgumentException("La hora programada debe ser en el futuro");
            if (scheduled > now + 7 * 1440) throw new IllegalArgumentException("Solo se puede programar hasta 7 días adelante");
        }
        double base = scheduled == null ? now : scheduled;
        double readyAt = base + readyMin, deadline = base + deadlineMin;
        double pickupDeadline = Math.min(readyAt + maxPickupMin, deadline - 1);

        String phone = clear(d.phone(), 20);
        if (!phone.isEmpty() && !PHONE_PATTERN.matcher(phone).matches()) throw new IllegalArgumentException("El teléfono del cliente no es válido");
        String customerName = clear(d.customer(), 80);
        Long customerId = d.customerId();
        if (customerId != null) {
            Customer c = db.customers().findById(customerId).orElseThrow(() -> new IllegalArgumentException("Ese cliente no existe"));
            if (customerName.isEmpty()) customerName = c.getName();
            if (phone.isEmpty()) phone = c.getPhone();
        } else if (Boolean.TRUE.equals(d.saveCustomer()) && !phone.isEmpty()) {
            if (customerName.isEmpty()) throw new IllegalArgumentException("Para guardar el cliente escribe su nombre");
            final String nom = customerName;
            Customer c = db.customers().findByPhone(phone).orElseGet(() -> db.customers().save(new Customer(nom, cleanPhone(d.phone()))));
            customerId = c.getId();
        }
        if (customerId != null && !clear(d.dropoffAddress(), 150).isEmpty()) saveAddress(customerId, clear(d.dropoffAddress(), 150), d.dropoff());

        PackageSize size = d.size() == null ? PackageSize.SMALL : d.size();
        Priority priority = d.priority() == null ? Priority.NORMAL : d.priority();
        PaymentMethod payment = d.paymentMethod() == null ? PaymentMethod.PREPAID : d.paymentMethod();
        long value = d.value() != null ? d.value() : suggestedValue(d.pickup(), d.dropoff(), size);
        if (value < 0 || value > 10_000_000) throw new IllegalArgumentException("El valor del servicio no es válido");
        String pin = String.format("%04d", random.nextInt(10000));

        Delivery s = new Delivery(d.pickup(), clear(d.pickupAddress(), 150), d.dropoff(), clear(d.dropoffAddress(), 150),
                customerId, customerName, phone, clear(d.note(), 200), size, priority, payment, value, pin,
                now, scheduled, readyAt, pickupDeadline, deadline, username);
        s.setCourierPay(rate == null ? 0L : rate.courierPay(value));
        s = db.deliveries().save(s);
        DeliveryStop r = db.stops().save(new DeliveryStop(s.getId(), StopType.PICKUP));
        DeliveryStop e = db.stops().save(new DeliveryStop(s.getId(), StopType.DROPOFF));
        r.takeChange(); e.takeChange();
        s.setStops(r, e);
        db.history().save(new StatusChange(s.getId(), null, DeliveryStatus.CREATED, now, username, "CENTRAL", null));
        deliveries.put(s.getId(), s);
        change(s, scheduled == null ? DeliveryStatus.QUEUED : DeliveryStatus.SCHEDULED, username, "CENTRAL",
                scheduled == null ? null : "Programado para las " + hhmm(scheduled), now);
        audit(username, "CREAR_SERVICIO", "SERVICIO", s.getId(), "Valor " + value + " COP", false);
        tryAssign();
        changed();
        return s;
    }

    /** Valor sugerido con la tarifa vigente (RF-M3-10). */
    public synchronized long suggestedValue(GeoPoint pickup, GeoPoint dropoff, PackageSize size) {
        if (rate == null) return 0;
        return rate.valueFor(routing.estimated().km(pickup, dropoff), size == null ? PackageSize.SMALL : size);
    }

    /** Edición mientras no se haya recogido (RF-M3-03). Si ya tiene domiciliario, se le avisa. */
    public synchronized void editDelivery(long id, EditData d, String username) {
        Delivery s = opened(id);
        if (s.getPickedUp() != null || s.getStatus() == DeliveryStatus.IN_PROGRESS)
            throw new IllegalArgumentException("El servicio #" + id + " ya fue recogido: no se puede editar");
        double readyAt = d.readyAt() == null ? s.getReadyAt() : d.readyAt();
        double deadline = d.deadline() == null ? s.getDeadline() : d.deadline();
        if (readyAt >= deadline) throw new IllegalArgumentException("El pedido debe estar listo antes del límite de entrega");
        String phone = clear(d.phone(), 20);
        if (!phone.isEmpty() && !PHONE_PATTERN.matcher(phone).matches()) throw new IllegalArgumentException("El teléfono del cliente no es válido");
        long value = d.value() == null ? s.getValue() : d.value();
        if (value < 0) throw new IllegalArgumentException("El valor no puede ser negativo");
        PackageSize size = d.size() == null ? s.getSize() : d.size();
        PackageSize before = s.getSize();
        double pickupDeadline = Math.min(readyAt + (s.getPickupDeadline() - s.getReadyAt()), deadline - 1);
        s.edit(clear(d.note(), 200), clear(d.customer(), 80), phone, size,
                d.priority() == null ? s.getPriority() : d.priority(), d.paymentMethod() == null ? s.getPaymentMethod() : d.paymentMethod(),
                value, readyAt, pickupDeadline, deadline);
        Courier owner = courierOf(s);
        if (owner != null && size != before) {
            RouteEvaluation ev = engine.evaluator().evaluate(owner, owner.getRoute(), environment(distances()));
            if (!ev.ok()) {
                s.edit(s.getNote(), s.getCustomer(), s.getPhone(), before, s.getPriority(), s.getPaymentMethod(), s.getValue(),
                        s.getReadyAt(), s.getPickupDeadline(), s.getDeadline());
                throw new IllegalArgumentException("Con ese tamaño ya no le cabe a " + owner.getName() + " (" + ev.reason() + ")");
            }
        }
        if (rate != null) s.setCourierPay(rate.courierPay(value));
        db.deliveries().save(s);
        audit(username, "EDITAR_SERVICIO", "SERVICIO", s.getId(), null, true);
        if (owner != null) notifyCourier(owner, "EDITED", Map.of("deliveryId", s.getId(), "text", "La central cambió los datos del pedido #" + s.getId()));
        changed();
    }

    /** Asignación manual (RF-M3-05) o reasignación con motivo (RF-M3-06). La manual prevalece sobre el motor (RN-15). */
    public synchronized void assignManually(long id, long courierId, String reason, String username) {
        Delivery s = opened(id);
        Courier d = courier(courierId);
        String m = clear(reason, 300);
        if (!d.isConnected()) throw new IllegalArgumentException(d.getName() + " está fuera de servicio");
        if (accountOf.get(d.getId()) != AccountStatus.ACTIVE) throw new IllegalArgumentException("La cuenta de " + d.getName() + " no está activa");
        if (vehicleOf.get(d.getId()) == null) throw new IllegalArgumentException(d.getName() + " no tiene vehículo registrado");
        Courier previous = null;
        switch (s.getStatus()) {
            case QUEUED -> { }
            case OFFERED -> voidOffers(s, "La central lo asignó a mano");
            case ASSIGNED -> {
                previous = courierOf(s);
                if (previous == d) throw new IllegalArgumentException("El servicio #" + id + " ya es de " + d.getName());
                if (m.isEmpty()) throw new IllegalArgumentException("Para reasignar escribe el motivo");
            }
            case IN_PROGRESS -> throw new IllegalArgumentException("El servicio #" + id + " ya fue recogido: no se puede reasignar");
            default -> throw new IllegalArgumentException("El servicio #" + id + " no se puede asignar en estado " + DeliveryStateMachine.text(s.getStatus()));
        }
        Environment e = environment(distances());
        AssignmentEngine.Candidate c = engine.bestInsertion(d, s, e, false, 0);
        if (c == null) throw new IllegalArgumentException("No le cabe a " + d.getName() + ": supera la capacidad de su vehículo");
        if (previous != null) {
            previous.removeDelivery(s);
            updateRoute(previous, e);
            notifyCourier(previous, "REVOKED", Map.of("deliveryId", s.getId(), "text", "La central le pasó el pedido #" + s.getId() + " a otro domiciliario"));
        }
        d.setRoute(c.route());
        s.assignTo(d.getId(), AssignmentOrigin.MANUAL);
        change(s, DeliveryStatus.ASSIGNED, username, "CENTRAL", previous != null ? "Reasignado: " + m : (m.isEmpty() ? "Asignación manual" : m), clock.now());
        rejectedBy.remove(s.getId());
        updateRoute(d, e);
        audit(username, previous != null ? "REASIGNAR" : "ASIGNAR_MANUAL", "SERVICIO", s.getId(), "A " + d.getUsername() + (m.isEmpty() ? "" : ": " + m), true);
        notifyCourier(d, "ASSIGNED", Map.of("deliveryId", s.getId(), "text", "La central te asignó el pedido #" + s.getId()));
        changed();
    }

    /** Revocar: le quita el servicio al domiciliario y lo devuelve a la cola, con motivo (RF-M3-06). */
    public synchronized void revoke(long id, String reason, String username) {
        Delivery s = opened(id);
        String m = requireReason(reason, "revocar");
        if (s.getStatus() == DeliveryStatus.IN_PROGRESS)
            throw new IllegalArgumentException("El servicio #" + id + " ya fue recogido: para devolverlo usa Cancelar (devolución)");
        if (s.getStatus() != DeliveryStatus.ASSIGNED) throw new IllegalArgumentException("Solo se revoca un servicio asignado");
        Courier d = courierOf(s);
        releaseFromCourier(s, d);
        change(s, DeliveryStatus.QUEUED, username, "CENTRAL", "Revocado: " + m, clock.now());
        if (d != null) {
            rejectedBy.computeIfAbsent(s.getId(), k -> new HashSet<>()).add(d.getId());
            notifyCourier(d, "REVOKED", Map.of("deliveryId", s.getId(), "text", "La central te quitó el pedido #" + s.getId() + ": " + m));
        }
        audit(username, "REVOCAR", "SERVICIO", s.getId(), m, true);
        tryAssign();
        changed();
    }

    /**
     * Cancelar con motivo (RF-M3-04). Si el domiciliario ya lo recogió, se abre una devolución (RN-07):
     * su "entrega" pasa a ser volver al punto de recogida y se crea una novedad.
     */
    public synchronized void cancel(long id, String reason, String username) {
        Delivery s = opened(id);
        String m = requireReason(reason, "cancelar");
        double now = clock.now();
        Courier d = courierOf(s);
        switch (s.getStatus()) {
            case CREATED, SCHEDULED, QUEUED -> { }
            case OFFERED -> voidOffers(s, "Servicio cancelado");
            case ASSIGNED -> releaseFromCourier(s, d);
            case IN_PROGRESS -> {
                s.markReturn(m);
                Incident inc = db.incidents().save(new Incident(IncidentType.RETURN, s.getId(), s.getCourierId(), username,
                        "Cancelado con el pedido a bordo: " + m + ". El domiciliario debe devolverlo al punto de recogida.", null, now));
                openIncidents++;
                audit(username, "ABRIR_DEVOLUCION", "INCIDENTE", inc.getId(), null, true);
            }
            default -> throw new IllegalArgumentException("El servicio #" + id + " ya terminó");
        }
        s.setCloseReason(m);
        change(s, DeliveryStatus.CANCELLED, username, "CENTRAL", m, now);
        if (d != null) {
            if (s.isReturnTrip()) {
                updateRoute(d, environment(distances()));
                notifyCourier(d, "RETURN", Map.of("deliveryId", s.getId(), "text", "Se canceló el pedido #" + s.getId() + ": devuélvelo al punto de recogida. Motivo: " + m));
            } else {
                notifyCourier(d, "CANCELLED", Map.of("deliveryId", s.getId(), "text", "Se canceló el pedido #" + s.getId() + ": " + m));
            }
        }
        audit(username, "CANCELAR", "SERVICIO", s.getId(), m, true);
        changed();
    }

    /** Cambia el modo del motor (RF-M5-01). */
    public synchronized void changeMode(AssignmentMode mode, String username) {
        settings.update("motor.modo", mode.dbCode());
        engineFailure = null;
        audit(username, "CAMBIAR_MODO", "MOTOR", null, mode.name(), false);
        if (mode == AssignmentMode.MANUAL) for (Offer o : new ArrayList<>(offers.values())) {
            closeOffer(o, OfferStatus.VOIDED, "La central pasó a modo manual");
            Delivery s = deliveries.get(o.getDeliveryId());
            if (s != null && s.getStatus() == DeliveryStatus.OFFERED && offersOf(s).isEmpty())
                change(s, DeliveryStatus.QUEUED, username, "CENTRAL", "Modo manual", clock.now());
        }
        tryAssign();
        changed();
    }

    /** El administrador cambia un parámetro de operación (RF-M10-06). Se aplica de inmediato y queda en auditoría. */
    public synchronized void changeSetting(String key, String value, String username) {
        if ("motor.modo".equals(key)) {
            AssignmentMode m;
            try { m = DbCoded.fromDbCode(AssignmentMode.class, value); }
            catch (IllegalArgumentException e) { throw new IllegalArgumentException("Modo inválido: usa MANUAL, DIFUSION o AUTOMATICO"); }
            changeMode(m, username);
            return;
        }
        settings.update(key, value);
        audit(username, "CAMBIAR_PARAMETRO", "PARAMETRO", null, key + " = " + value, false);
        for (Courier d : couriers.values()) refreshEligibility(d);
        tryAssign();
        changed();
    }

    // =====================================================================================
    // Domiciliario: estado, ubicación, ofertas y paradas
    // =====================================================================================

    /** Disponible, Ocupado (pausa) o Fuera de servicio (RF-M6-02). Al ponerse disponible se abre su turno. */
    public synchronized void changeStatus(long courierId, CourierStatus newStatus, GeoPoint position, Double accuracyM) {
        Courier d = courier(courierId);
        double now = clock.now();
        CourierStatus current = d.getCourierStatus();
        if (newStatus == CourierStatus.ON_ROUTE) throw new IllegalArgumentException("El estado En ruta lo pone el sistema cuando tienes paradas");
        Environment e = environment(distances());

        if (newStatus == CourierStatus.OFF_DUTY) {
            if (current == CourierStatus.OFF_DUTY) return;
            if (d.carriesOrders()) throw new IllegalArgumentException("Primero entrega los pedidos que llevas (RN-14)");
            for (Stop p : new ArrayList<>(d.getRoute())) {
                Delivery s = p.delivery();
                if (s.getStatus() == DeliveryStatus.ASSIGNED) {
                    releaseFromCourier(s, d);
                    change(s, DeliveryStatus.QUEUED, d.getUsername(), "DOMICILIARIO", "El domiciliario salió de servicio", now);
                }
            }
            Offer pending = offerOf(d);
            if (pending != null) closeOfferAndCountRejection(pending, OfferStatus.REJECTED, "Salió de servicio");
            d.setRoute(List.of());
            Shift t = shifts.remove(d.getId());
            if (t != null) { t.close(now); db.shifts().save(t); }
            d.endShift();
            d.setCourierStatus(CourierStatus.OFF_DUTY);
            updateRoute(d, e);
            db.couriers().save(d);
            tryAssign();
            changed();
            return;
        }

        if (current == CourierStatus.OFF_DUTY) {
            refreshEligibility(d);
            AccountStatus account = accountOf.get(d.getId());
            if (account != AccountStatus.ACTIVE) throw new IllegalArgumentException("Tu cuenta no está activa: habla con la central");
            if (position == null) throw new IllegalArgumentException("Necesitamos tu ubicación GPS para empezar: activa el permiso de ubicación");
            if (!position.isValid()) throw new IllegalArgumentException("La ubicación enviada no es válida");
            d.setGps(position, accuracyM, now);
            Shift t = db.shifts().save(new Shift(d.getId(), now));
            shifts.put(d.getId(), t);
            d.startShift(now, t.getId());
            db.locations().save(new GpsLocation(d.getId(), position, accuracyM, newStatus, now, now));
        }
        if (newStatus == CourierStatus.BUSY) {
            Offer pending = offerOf(d);
            if (pending != null) closeOfferAndCountRejection(pending, OfferStatus.REJECTED, "Se puso en pausa");
        }
        d.setCourierStatus(newStatus == CourierStatus.AVAILABLE && !d.getRoute().isEmpty() ? CourierStatus.ON_ROUTE : newStatus);
        d.signal(now);
        db.couriers().save(d);
        if (newStatus == CourierStatus.AVAILABLE) d.resetRejections();
        tryAssign();
        changed();
    }

    /** GPS real: el celular manda su posición (RF-M6-10). Solo se acepta mientras trabaja (RN-13). */
    public synchronized void recordLocation(long courierId, GeoPoint position, Double accuracyM, Double captureTime) {
        if (position == null || !position.isValid()) throw new IllegalArgumentException("La ubicación enviada no es válida");
        Courier d = courier(courierId);
        if (!d.isConnected()) throw new IllegalArgumentException("Ponte disponible para compartir tu ubicación");
        double now = clock.now();
        d.signal(now);
        if (accuracyM != null && accuracyM > settings.maxAccuracyM()) return;     // muy impreciso: cuenta como señal, no mueve
        double capture = captureTime != null && captureTime <= now + 0.1 && captureTime > now - 1440 ? captureTime : now;
        addKm(d, position);
        d.setGps(position, accuracyM, now);
        db.locations().save(new GpsLocation(d.getId(), position, accuracyM, d.getCourierStatus(), capture, now));
        db.couriers().save(d);
        checkArrival(d);
        changed();
    }

    /** Aceptar o rechazar una oferta (RF-M6-03). La aceptación es atómica: el primero gana (RN-02). */
    public synchronized void replyToOffer(long courierId, long offerId, boolean accept, String reason) {
        Courier d = courier(courierId);
        Offer o = offers.get(offerId);
        if (o == null || o.getCourierId() != courierId) throw new IllegalArgumentException("Esa oferta ya no está disponible");
        double now = clock.now();
        if (now > o.getDeadline()) {
            closeOfferAndCountRejection(o, OfferStatus.EXPIRED, "Se venció el tiempo");
            changed();
            throw new IllegalArgumentException("La oferta ya venció");
        }
        Delivery s = deliveries.get(o.getDeliveryId());
        if (!accept) {
            closeOfferAndCountRejection(o, OfferStatus.REJECTED, clear(reason, 200));
            tryAssign();
            changed();
            return;
        }
        if (s == null || s.getStatus() != DeliveryStatus.OFFERED) {
            closeOffer(o, OfferStatus.VOIDED, "Otro domiciliario ya lo tomó");
            changed();
            throw new IllegalArgumentException("Otro domiciliario ya tomó ese pedido");
        }
        Environment e = environment(distances());
        AssignmentEngine.Candidate c = engine.bestInsertion(d, s, e, false, 0);
        if (c == null) {
            closeOfferAndCountRejection(o, OfferStatus.REJECTED, "Ya no le cabía");
            changed();
            throw new IllegalArgumentException("Ya no te cabe ese pedido (capacidad del vehículo)");
        }
        closeOffer(o, OfferStatus.ACCEPTED, null);
        voidOffers(s, "Lo tomó otro domiciliario");
        d.setRoute(c.route());
        s.assignTo(d.getId(), o.getMode() == AssignmentMode.BROADCAST ? AssignmentOrigin.BROADCAST : AssignmentOrigin.AUTOMATIC);
        change(s, DeliveryStatus.ASSIGNED, d.getUsername(), "DOMICILIARIO", "Aceptó la oferta", now);
        d.resetRejections();
        rejectedBy.remove(s.getId());
        updateRoute(d, e);
        changed();
    }

    /**
     * "Recogí" / "Entregué" (RF-M6-05). Solo vale cerca de la parada. Al entregar exige la prueba (RN-09)
     * y, si es contra entrega, lo cobrado (RN-10). Sirve también para eventos que llegan tarde (sin conexión, RF-M6-11).
     */
    public synchronized String confirmStop(long courierId, Confirmation c) {
        if (c == null) c = Confirmation.simple();
        if (c.eventId() != null && !c.eventId().isBlank() && db.events().existsById(c.eventId())) return "Ya estaba registrado";
        Courier d = courier(courierId);
        Stop p = d.nextStop();
        if (p == null) throw new IllegalArgumentException("No tienes paradas pendientes");
        double now = clock.now();
        double time = c.eventTime() != null && c.eventTime() <= now + 0.1 && c.eventTime() > now - 1440 ? c.eventTime() : now;
        GeoPoint pos = c.lat() != null && c.lng() != null ? new GeoPoint(c.lat(), c.lng()) : d.getPosition();
        if (!pos.isValid()) pos = d.getPosition();
        double meters = pos.distanceM(p.destination());
        if (meters > settings.arrivalMeters())
            throw new IllegalArgumentException("Todavía no llegaste: estás a " + Math.round(meters) + " m (debes estar a menos de " + Math.round(settings.arrivalMeters()) + " m)");
        Delivery s = p.delivery();
        DeliveryStop ps = s.stop(p.type());
        d.signal(now);
        String result;

        if (p.type() == StopType.PICKUP) {
            if (time < s.getReadyAt()) throw new IllegalArgumentException("El pedido #" + s.getId() + " estará listo a las " + hhmm(s.getReadyAt()));
            change(s, DeliveryStatus.IN_PROGRESS, d.getUsername(), "DOMICILIARIO", null, time);
            ps.status(StopStatus.AT_STOP, time);
            ps.status(StopStatus.COMPLETED, time);
            result = "Pedido #" + s.getId() + " recogido";
        } else if (s.isReturnTrip()) {
            ps.status(StopStatus.AT_STOP, time);
            ps.status(StopStatus.COMPLETED, time);
            for (Incident inc : db.incidents().findByDeliveryIdOrderByIdAsc(s.getId()))
                if (inc.getType() == IncidentType.RETURN && inc.getStatus() != IncidentStatus.CLOSED)
                    db.followUps().save(new IncidentFollowUp(inc.getId(), d.getUsername(), "Devolución completada en el punto de recogida", time));
            audit(d.getUsername(), "DEVOLUCION_COMPLETADA", "SERVICIO", s.getId(), null, false);
            result = "Devolución del pedido #" + s.getId() + " registrada";
        } else {
            List<DeliveryProof> proofs = new ArrayList<>();
            String pin = c.pin() == null ? "" : c.pin().trim();
            if (settings.requirePin() || !pin.isEmpty()) {
                if (!s.getPin().equals(pin)) throw new IllegalArgumentException("PIN incorrecto: pídele al cliente el PIN de 4 dígitos del pedido #" + s.getId());
                proofs.add(new DeliveryProof(s.getId(), d.getId(), ProofType.PIN, "PIN verificado", time));
            }
            if (c.photo() != null && !c.photo().isBlank()) proofs.add(new DeliveryProof(s.getId(), d.getId(), ProofType.PHOTO, image(c.photo(), "foto"), time));
            else if (settings.requirePhoto()) throw new IllegalArgumentException("Toma una foto de la entrega");
            if (c.signature() != null && !c.signature().isBlank()) proofs.add(new DeliveryProof(s.getId(), d.getId(), ProofType.SIGNATURE, image(c.signature(), "firma"), time));
            else if (settings.requireSignature()) throw new IllegalArgumentException("Pídele la firma al cliente");
            if (proofs.isEmpty()) throw new IllegalArgumentException("Falta la prueba de entrega: PIN, foto o firma (RN-09)");
            Long amountCollected = null;
            if (s.getPaymentMethod().cashOnDelivery()) {
                if (c.amountReceived() == null) throw new IllegalArgumentException("Registra cuánto te pagó el cliente (pago contra entrega)");
                if (c.amountReceived() < 0 || c.amountReceived() > 50_000_000) throw new IllegalArgumentException("El valor recibido no es válido");
                amountCollected = c.amountReceived();
            }
            for (DeliveryProof ev : proofs) db.proofs().save(ev);
            long payment = s.getCourierPay() == null ? 0 : s.getCourierPay();
            s.recordCollection(amountCollected, payment);
            Shift t = shifts.get(d.getId());
            if (t != null) { t.addDelivered(amountCollected == null ? 0 : amountCollected, payment); db.shifts().save(t); }
            ps.status(StopStatus.AT_STOP, time);
            ps.status(StopStatus.COMPLETED, time);
            change(s, DeliveryStatus.DELIVERED, d.getUsername(), "DOMICILIARIO",
                    proofs.stream().map(x -> x.getType().dbCode()).collect(Collectors.joining(", ", "Prueba: ", "")), time);
            result = "Pedido #" + s.getId() + " entregado";
        }
        d.completeNextStop();
        saveStops(s);
        if (c.eventId() != null && !c.eventId().isBlank()) db.events().save(new ProcessedEvent(c.eventId().trim(), now));
        Environment e = environment(distances());
        d.setRoute(engine.reorder(d, e));
        updateRoute(d, e);
        tryAssign();
        changed();
        return result;
    }

    // =====================================================================================
    // Novedades / incidentes (RF-M6-08, RF-M7, RF-M4-05)
    // =====================================================================================

    public synchronized Incident reportIncident(Long courierId, String username, IncidentType type, Long deliveryId, String description, String photo) {
        if (type == null) throw new IllegalArgumentException("Escoge el tipo de novedad");
        String text = clear(description, 500);
        if (text.length() < 3) throw new IllegalArgumentException("Describe la novedad");
        if (deliveryId != null) {
            Delivery s = deliveries.get(deliveryId);
            if (s == null) throw new IllegalArgumentException("No existe el servicio #" + deliveryId);
            if (courierId != null && !Objects.equals(s.getCourierId(), courierId)) throw new IllegalArgumentException("El servicio #" + deliveryId + " no es tuyo");
        }
        String f = photo == null || photo.isBlank() ? null : image(photo, "foto");
        Incident inc = db.incidents().save(new Incident(type, deliveryId, courierId, username, text, f, clock.now()));
        openIncidents++;
        audit(username, "REPORTAR_NOVEDAD", "INCIDENTE", inc.getId(), type.name(), false);
        notifier.toCentral("INCIDENT", Map.of("id", inc.getId(), "text", type.text() + (deliveryId == null ? "" : " · pedido #" + deliveryId) + ": " + text));
        changed();
        return inc;
    }

    public synchronized void commentIncident(long id, String comment, String username) {
        Incident inc = db.incidents().findById(id).orElseThrow(() -> new IllegalArgumentException("No existe esa novedad"));
        String text = clear(comment, 500);
        if (text.isEmpty()) throw new IllegalArgumentException("Escribe el comentario");
        if (inc.getStatus() == IncidentStatus.CLOSED) throw new IllegalArgumentException("La novedad ya está cerrada");
        inc.attend();
        db.incidents().save(inc);
        db.followUps().save(new IncidentFollowUp(id, username, text, clock.now()));
        changed();
    }

    /** La central decide qué pasa con el servicio y cierra la novedad (RF-M7-04/05). */
    public synchronized void decideIncident(long id, IncidentDecision decision, String resolution, String username) {
        Incident inc = db.incidents().findById(id).orElseThrow(() -> new IllegalArgumentException("No existe esa novedad"));
        if (inc.getStatus() == IncidentStatus.CLOSED) throw new IllegalArgumentException("La novedad ya está cerrada");
        if (decision == null) throw new IllegalArgumentException("Escoge una decisión");
        String text = requireReason(resolution, "cerrar la novedad");
        Delivery s = inc.getDeliveryId() == null ? null : deliveries.get(inc.getDeliveryId());
        double now = clock.now();
        if (s != null) {
            Courier d = courierOf(s);
            switch (decision) {
                case RETRY -> {
                    if (s.getStatus() == DeliveryStatus.FAILED) {
                        if (d != null && d.getRoute().stream().anyMatch(p -> p.delivery() == s)) throw new IllegalArgumentException("Primero debe completarse la devolución");
                        s.getPickupStop().release(); s.getDropoffStop().release();
                        change(s, DeliveryStatus.QUEUED, username, "CENTRAL", "Reintento: " + text, now);
                        saveStops(s);
                    }
                }
                case RELEASE -> {
                    if (s.getStatus() != DeliveryStatus.ASSIGNED) throw new IllegalArgumentException("Solo se libera un servicio asignado que no se ha recogido");
                    releaseFromCourier(s, d);
                    change(s, DeliveryStatus.QUEUED, username, "CENTRAL", "Liberado por novedad: " + text, now);
                    if (d != null) notifyCourier(d, "REVOKED", Map.of("deliveryId", s.getId(), "text", "La central liberó el pedido #" + s.getId()));
                }
                case CANCEL -> { if (s.isOpen()) cancel(s.getId(), text, username); }
                case MARK_FAILED -> {
                    if (s.getStatus() == DeliveryStatus.ASSIGNED) {
                        releaseFromCourier(s, d);
                        change(s, DeliveryStatus.FAILED, username, "CENTRAL", text, now);
                    } else if (s.getStatus() == DeliveryStatus.IN_PROGRESS) {
                        s.markReturn(text);
                        change(s, DeliveryStatus.FAILED, username, "CENTRAL", text + " (devolución al punto de recogida)", now);
                        if (d != null) {
                            updateRoute(d, environment(distances()));
                            notifyCourier(d, "RETURN", Map.of("deliveryId", s.getId(), "text", "El pedido #" + s.getId() + " se marcó fallido: devuélvelo al punto de recogida"));
                        }
                    } else if (s.isOpen()) {
                        throw new IllegalArgumentException("Solo se marca fallido un servicio asignado o en camino");
                    }
                    s.setCloseReason(text);
                    db.deliveries().save(s);
                }
                case NO_ACTION -> { }
            }
        }
        inc.close(decision, text, username, now);
        db.incidents().save(inc);
        db.followUps().save(new IncidentFollowUp(id, username, "Cerrada: " + decision + " · " + text, now));
        openIncidents = db.incidents().findByStatusNotOrderByIdDesc(IncidentStatus.CLOSED).size();
        audit(username, "CERRAR_NOVEDAD", "INCIDENTE", id, decision.name(), true);
        tryAssign();
        changed();
    }

    // =====================================================================================
    // Mensajes (RF-M4-06) y recaudo (RF-M8-03)
    // =====================================================================================

    public synchronized ChatMessage messageToCourier(long courierId, String text, String author) {
        Courier d = courier(courierId);
        String t = clear(text, 300);
        if (t.isEmpty()) throw new IllegalArgumentException("Escribe el mensaje");
        ChatMessage m = db.messages().save(new ChatMessage(courierId, true, author, t, clock.now()));
        notifyCourier(d, "MESSAGE", Map.of("text", t, "author", author));
        changed();
        return m;
    }

    public synchronized ChatMessage messageToCentral(long courierId, String text) {
        Courier d = courier(courierId);
        String t = clear(text, 300);
        if (t.isEmpty()) throw new IllegalArgumentException("Escribe el mensaje");
        ChatMessage m = db.messages().save(new ChatMessage(courierId, false, d.getName(), t, clock.now()));
        unreadMessages.merge(courierId, 1, Integer::sum);
        notifier.toCentral("MESSAGE", Map.of("courierId", courierId, "text", d.getName() + ": " + t));
        changed();
        return m;
    }

    /** Conversación con un domiciliario (de la más vieja a la más nueva). leidoPorCentral marca como leídos los suyos. */
    public synchronized List<MessageView> conversation(long courierId, boolean readByCentral) {
        courier(courierId);
        List<ChatMessage> ms = new ArrayList<>(db.messages().findTop40ByCourierIdOrderByDateDesc(courierId));
        Collections.reverse(ms);
        boolean anyChanged = false;
        for (ChatMessage m : ms) {
            if (!m.isRead() && m.isFromCentral() != readByCentral) { m.markRead(); db.messages().save(m); anyChanged = true; }
        }
        if (readByCentral && unreadMessages.remove(courierId) != null) anyChanged = true;
        if (anyChanged) changed();
        return ms.stream().map(this::messageView).toList();
    }

    /** La central recibe el dinero cobrado por el domiciliario (cuadre, RF-M8-03). Las diferencias quedan en la auditoría. */
    public synchronized Shift receiveCollection(long courierId, long value, String username) {
        Courier d = courier(courierId);
        if (value < 0) throw new IllegalArgumentException("El valor no puede ser negativo");
        Shift t = shifts.get(courierId);
        if (t == null) t = db.shifts().findAll().stream().filter(x -> x.getCourierId() == courierId && x.getPending() > 0)
                .max(Comparator.comparing(Shift::getId)).orElse(null);
        if (t == null) throw new IllegalArgumentException(d.getName() + " no tiene recaudo pendiente");
        long pending = t.getPending();
        t.receiveCollection(value);
        db.shifts().save(t);
        long difference = value - pending;
        audit(username, "RECIBIR_RECAUDO", "TURNO", t.getId(),
                "Recibido " + value + " de " + pending + " COP" + (difference == 0 ? "" : " · diferencia " + difference), false);
        changed();
        return t;
    }

    // =====================================================================================
    // Datos maestros que afectan la operación
    // =====================================================================================

    /** Una cuenta aprobada entra a la operación (o se actualiza su nombre). */
    public synchronized void registerCourier(long id, String name, String username, String idNumber, String phone) {
        // OJO: save() de una entidad con id asignado devuelve una COPIA; en memoria se conserva siempre la misma instancia
        Courier d = couriers.get(id);
        if (d == null) d = db.couriers().findById(id).orElse(null);
        if (d == null) d = new Courier(id, name, username, area.getCenter());
        d.updateData(name, idNumber == null ? d.getIdNumber() : idNumber, phone == null ? d.getPhone() : phone);
        d.setUsername(username);
        if (d.getPosition() == null) d.setPosition(area.getCenter());
        db.couriers().save(d);
        if (!couriers.containsKey(id)) d.signal(clock.now());
        couriers.put(id, d);
        refreshEligibility(d);
        changed();
    }

    /** Vehículo o estado de cuenta cambiaron: recalcula si puede recibir ofertas (RN-03). */
    public synchronized void refreshCourier(long id) {
        Courier d = couriers.get(id);
        if (d == null) return;
        refreshEligibility(d);
        AccountStatus account = accountOf.get(id);
        if (account != AccountStatus.ACTIVE && d.isConnected()) {
            if (!d.getRoute().isEmpty()) return;               // con servicios activos sigue hasta terminarlos
            Shift t = shifts.remove(id);
            if (t != null) { t.close(clock.now()); db.shifts().save(t); }
            d.endShift();
            d.setCourierStatus(CourierStatus.OFF_DUTY);
            db.couriers().save(d);
        }
        changed();
    }

    /** La cuenta se va a suspender/desactivar: no puede tener servicios activos. */
    public synchronized void validateNoDeliveries(long id) {
        Courier d = couriers.get(id);
        if (d != null && !d.getRoute().isEmpty())
            throw new IllegalArgumentException(d.getName() + " tiene servicios activos: revócalos o espera a que termine");
    }

    public synchronized void rateChanged() {
        rate = db.rates().findFirstByActiveTrueOrderByIdDesc().orElse(null);
    }

    public synchronized Rate currentRate() { return rate; }

    // =====================================================================================
    // Planificador: corre cada segundo
    // =====================================================================================

    public synchronized void tick() {
        double now = clock.now();
        // 1. Ofertas vencidas (RN-05)
        for (Offer o : new ArrayList<>(offers.values()))
            if (now > o.getDeadline()) closeOfferAndCountRejection(o, OfferStatus.EXPIRED, "Se venció el tiempo");
        // 2. Servicios programados que ya deben entrar a la cola (RF-M3-08)
        for (Delivery s : new ArrayList<>(deliveries.values()))
            if (s.getStatus() == DeliveryStatus.SCHEDULED && s.getScheduledFor() != null && s.getScheduledFor() <= now)
                change(s, DeliveryStatus.QUEUED, "sistema", "SISTEMA", "Llegó la hora programada", now);
        // 3. Recogida vencida: prioridad máxima (RN-12)
        for (Delivery s : deliveries.values()) {
            if (s.isOpen() && s.getPickedUp() == null && s.getStatus() != DeliveryStatus.SCHEDULED
                    && now > s.getPickupDeadline() && s.getPriority() != Priority.URGENT) {
                s.setPriority(Priority.URGENT);
                db.deliveries().save(s);
                audit("sistema", "PRIORIDAD_MAXIMA", "SERVICIO", s.getId(), "Venció el tiempo máximo de recogida", false);
            }
        }
        // 4. Llegadas a las paradas (EN_PUNTO)
        for (Courier d : couriers.values()) if (d.isConnected()) checkArrival(d);
        // 5. Motor: ofertas para lo que está en cola
        tryAssign();
        // 6. Reordenar rutas cada N segundos (RF-M5-07) y actualizar horas estimadas
        if ((now - lastReorder) * 60 >= settings.number("motor.reordenar-cada-seg")) {
            lastReorder = now;
            try {
                List<Courier> withRoute = couriers.values().stream().filter(d -> !d.getRoute().isEmpty()).toList();
                if (!withRoute.isEmpty()) {
                    Environment e = environment(matrixFor(withRoute, List.of()));
                    for (Courier d : withRoute) { d.setRoute(engine.reorder(d, e)); updateRoute(d, e); }
                }
            } catch (RuntimeException ex) { engineFailed(ex); }
        }
        // 7. Cada 10 minutos: elegibilidad; cada hora: depurar GPS viejo (RNF-10); limpiar cerrados de memoria
        if (now - lastEligibility >= 10) {
            lastEligibility = now;
            for (Courier d : couriers.values()) refreshEligibility(d);
            deliveries.values().removeIf(s -> !s.isOpen() && s.getClosed() != null && s.getClosed() < now - 1440
                    && couriers.values().stream().noneMatch(d -> d.getRoute().stream().anyMatch(p -> p.delivery() == s)));
        }
        if (now - lastCleanup >= 60) {
            lastCleanup = now;
            try { db.locations().deleteByCapturedAtLessThan(now - settings.number("gps.retencion-dias") * 1440); }
            catch (RuntimeException ex) { log.warn("No se pudo depurar el historial de GPS: {}", ex.getMessage()); }
        }
        changed();
    }

    // =====================================================================================
    // Motor: ofertas automáticas o por difusión (RF-M5-01, RF-M5-03, RF-M5-04)
    // =====================================================================================

    /** Corre el motor protegido: si falla, pasa a modo Manual y avisa (RF-M5-08). */
    private void tryAssign() {
        if (settings.mode() == AssignmentMode.MANUAL) return;
        try { assignPending(); } catch (RuntimeException ex) { engineFailed(ex); }
    }

    private void engineFailed(RuntimeException ex) {
        log.error("Falló el motor de asignación; se pasa a modo MANUAL", ex);
        engineFailure = "El motor falló a las " + hhmm(clock.now()) + " (" + ex.getClass().getSimpleName()
                + (ex.getMessage() == null ? "" : ": " + ex.getMessage()) + "). Se pasó a modo Manual.";
        try { settings.update("motor.modo", AssignmentMode.MANUAL.dbCode()); } catch (RuntimeException ignored) { }
        audit("sistema", "MOTOR_FALLO", "MOTOR", null, engineFailure, false);
        notifier.toCentral("ENGINE", Map.of("text", engineFailure));
    }

    private void assignPending() {
        AssignmentMode mode = settings.mode();
        if (mode == AssignmentMode.MANUAL) return;
        double now = clock.now();
        List<Delivery> queue = deliveries.values().stream().filter(s -> s.getStatus() == DeliveryStatus.QUEUED)
                .sorted(urgencyOrder(now)).toList();
        if (queue.isEmpty()) return;
        List<Courier> eligibles = new ArrayList<>(couriers.values().stream().filter(d -> eligibleForOffer(d, now)).toList());
        if (eligibles.isEmpty()) return;

        Environment e = environment(matrixFor(eligibles, queue));
        int byDelivery = mode == AssignmentMode.BROADCAST ? settings.broadcastCandidates() : 1;
        double seconds = settings.offerSeconds();
        for (Delivery s : queue) {
            if (eligibles.isEmpty()) break;
            Set<Long> excluded = rejectedBy.getOrDefault(s.getId(), Set.of());
            List<Courier> possible = eligibles.stream().filter(d -> !excluded.contains(d.getId())).toList();
            if (possible.isEmpty()) continue;
            List<AssignmentEngine.Candidate> cands = engine.candidates(s, possible, e, settings.maxDetourKm());
            if (cands.isEmpty()) continue;
            List<AssignmentEngine.Candidate> chosen = cands.subList(0, Math.min(byDelivery, cands.size()));
            for (AssignmentEngine.Candidate c : chosen) {
                Offer o = db.offers().save(new Offer(s.getId(), c.courier().getId(), mode, now, now + seconds / 60.0, c.extraCost()));
                offers.put(o.getId(), o);
                offerInfo.put(o.getId(), new double[]{ c.kmExtra(), c.eval().schedules().stream()
                        .filter(h -> h.stop().delivery() == s && h.stop().type() == StopType.DROPOFF).mapToDouble(Schedule::sale).findFirst().orElse(0) });
                eligibles.remove(c.courier());
                notifyCourier(c.courier(), "OFFER", Map.of("offerId", o.getId(), "deliveryId", s.getId(), "seconds", (int) seconds,
                        "text", "Nuevo pedido #" + s.getId() + ": tienes " + (int) seconds + " s para aceptar"));
            }
            String candidates = cands.stream().limit(15).map(c -> c.courier().getId() + ":" + String.format(Locale.ROOT, "%.1f", c.extraCost()))
                    .collect(Collectors.joining(";"));
            db.decisions().save(new EngineDecision(s.getId(), now, mode, chosen.get(0).courier().getId(), chosen.get(0).extraCost(), candidates));
            String currentUser = chosen.stream().map(c -> c.courier().getUsername()).collect(Collectors.joining(", "));
            change(s, DeliveryStatus.OFFERED, "motor", "MOTOR", "Ofrecido a " + currentUser, now);
        }
    }

    /** Puede recibir una oferta nueva (RN-03): cuenta activa, vehículo, disponible o en ruta, con señal y sin otra oferta. */
    private boolean eligibleForOffer(Courier d, double now) {
        CourierStatus e = d.getCourierStatus();
        return d.isCanReceiveOffers() && (e == CourierStatus.AVAILABLE || e == CourierStatus.ON_ROUTE)
                && now - d.getLastSignal() <= settings.minutesWithoutSignal() && offerOf(d) == null && d.getPosition() != null;
    }

    /** Más urgente primero: prioridad, luego el que vence antes, luego el que lleva más esperando (RF-M5-04). */
    private Comparator<Delivery> urgencyOrder(double now) {
        return Comparator.comparing((Delivery s) -> -s.getPriority().ordinal())
                .thenComparingDouble(s -> Math.min(s.getPickupDeadline(), s.getDeadline()))
                .thenComparingDouble(s -> s.getQueuedSince() == null ? now : s.getQueuedSince());
    }

    private Distances matrixFor(List<Courier> ds, List<Delivery> extra) {
        List<GeoPoint> points = new ArrayList<>();
        for (Courier d : ds) {
            if (d.getPosition() != null) points.add(d.getPosition());
            for (Stop p : d.getRoute()) points.add(p.destination());
        }
        for (Delivery s : extra) { points.add(s.getPickup()); points.add(s.getDropoff()); }
        lastMatrix = routing.target(points);
        return lastMatrix;
    }

    private Distances distances() { return lastMatrix != null ? lastMatrix : routing.estimated(); }

    private Environment environment(Distances d) {
        return new Environment(clock.now(), d, settings.pickupMin(), settings.dropoffMin(), reserveActive());
    }

    /** Hay reserva activa si, según los pedidos de los últimos 10 min, conviene dejar domiciliarios libres. */
    private boolean reserveActive() {
        if (!settings.flag("motor.reserva-libres")) return false;
        long working = couriers.values().stream().filter(Courier::isConnected).count();
        if (working < 2) return false;
        double now = clock.now();
        double pace = deliveries.values().stream().filter(s -> s.getCreated() > now - 10).count() / 10.0;
        return pace * 8 / 3 >= 1;
    }

    /** Kilómetros recorridos en el turno (indicador I4). Saltos de más de 3 km entre dos puntos se ignoran (GPS que "brinca"). */
    private void addKm(Courier d, GeoPoint newStatus) {
        Shift t = shifts.get(d.getId());
        if (t == null || d.getPosition() == null) return;
        double km = d.getPosition().distanceKm(newStatus);
        if (km > 0.005 && km < 3) { t.addKm(km); db.shifts().save(t); }
    }

    // =====================================================================================
    // Rutas y paradas
    // =====================================================================================

    /** Guarda el orden y la hora estimada de cada parada, abre/cierra la RUTA y ajusta EN_RUTA ↔ DISPONIBLE. */
    private void updateRoute(Courier d, Environment e) {
        double now = clock.now();
        boolean courierChanged = false;
        if (!d.getRoute().isEmpty() && d.getRouteId() == null) {
            Route r = db.routes().save(new Route(d.getId(), now));
            d.setRouteId(r.getId());
            courierChanged = true;
        } else if (d.getRoute().isEmpty() && d.getRouteId() != null) {
            db.routes().findById(d.getRouteId()).ifPresent(r -> { r.close(now); db.routes().save(r); });
            d.setRouteId(null);
            courierChanged = true;
        }
        if (!d.getRoute().isEmpty()) {
            RouteEvaluation ev = engine.evaluator().evaluate(d, d.getRoute(), e);
            List<Schedule> hs = ev.schedules();
            Set<Delivery> touched = new HashSet<>();
            for (int i = 0; i < hs.size(); i++) {
                Schedule h = hs.get(i);
                Delivery s = h.stop().delivery();
                DeliveryStop ps = s.stop(h.stop().type());
                ps.locate(d.getId(), d.getRouteId(), i + 1, h.arrives());
                if (i == 0 && ps.getStatus() == StopStatus.PENDING) ps.status(StopStatus.ON_THE_WAY, now);
                if (i > 0 && ps.getStatus() == StopStatus.ON_THE_WAY) ps.status(StopStatus.PENDING, now);
                touched.add(s);
            }
            for (Delivery s : touched) saveStops(s);
        }
        if (!d.getRoute().isEmpty() && d.getCourierStatus() == CourierStatus.AVAILABLE) { d.setCourierStatus(CourierStatus.ON_ROUTE); courierChanged = true; }
        if (d.getRoute().isEmpty() && d.getCourierStatus() == CourierStatus.ON_ROUTE) { d.setCourierStatus(CourierStatus.AVAILABLE); courierChanged = true; }
        if (courierChanged) db.couriers().save(d);
    }

    /** Si llegó a menos de N metros de su siguiente parada, la marca EN_PUNTO (sección 6.2). */
    private void checkArrival(Courier d) {
        Stop p = d.nextStop();
        if (p == null || d.getPosition() == null) return;
        DeliveryStop ps = p.delivery().stop(p.type());
        if (ps == null) return;
        if (ps.getStatus() != StopStatus.AT_STOP && d.getPosition().distanceM(p.destination()) <= settings.arrivalMeters()) {
            ps.status(StopStatus.AT_STOP, clock.now());
            saveStops(p.delivery());
        }
    }

    /** Saca el servicio de la ruta de su domiciliario y deja sus paradas libres. */
    private void releaseFromCourier(Delivery s, Courier d) {
        if (d != null) {
            d.removeDelivery(s);
            updateRoute(d, environment(distances()));
        }
        s.getPickupStop().release();
        s.getDropoffStop().release();
        saveStops(s);
    }

    private void saveStops(Delivery s) {
        for (DeliveryStop p : List.of(s.getPickupStop(), s.getDropoffStop()))
            if (p != null && p.takeChange()) db.stops().save(p);
    }

    // =====================================================================================
    // Ofertas
    // =====================================================================================

    private Offer offerOf(Courier d) {
        for (Offer o : offers.values()) if (o.getCourierId().equals(d.getId())) return o;
        return null;
    }

    private List<Offer> offersOf(Delivery s) {
        List<Offer> list = new ArrayList<>();
        for (Offer o : offers.values()) if (o.getDeliveryId().equals(s.getId())) list.add(o);
        return list;
    }

    private void closeOffer(Offer o, OfferStatus result, String reason) {
        o.close(result, clock.now(), reason);
        db.offers().save(o);
        offers.remove(o.getId());
        offerInfo.remove(o.getId());
        Courier d = couriers.get(o.getCourierId());
        if (d != null && result != OfferStatus.ACCEPTED)
            notifyCourier(d, "OFFER_CLOSED", Map.of("offerId", o.getId(), "text", reason == null ? "La oferta se cerró" : reason));
    }

    /** Rechazo o vencimiento: se anota, se cuenta para la alerta (RN-05) y, si nadie más la tiene, el servicio vuelve a la cola. */
    private void closeOfferAndCountRejection(Offer o, OfferStatus result, String reason) {
        closeOffer(o, result, reason);
        Delivery s = deliveries.get(o.getDeliveryId());
        Courier d = couriers.get(o.getCourierId());
        if (s != null) rejectedBy.computeIfAbsent(s.getId(), k -> new HashSet<>()).add(o.getCourierId());
        if (d != null) {
            int consecutive = d.addRejection();
            if (consecutive == settings.rejectionsAlert())
                audit("sistema", "RECHAZOS_REITERADOS", "DOMICILIARIO", d.getId(), consecutive + " rechazos o vencimientos seguidos", false);
        }
        if (s != null && s.getStatus() == DeliveryStatus.OFFERED && offersOf(s).isEmpty()) {
            String currentUser = d == null ? "" : " por " + d.getUsername();
            change(s, DeliveryStatus.QUEUED, d == null ? "sistema" : d.getUsername(), result == OfferStatus.EXPIRED ? "SISTEMA" : "DOMICILIARIO",
                    (result == OfferStatus.EXPIRED ? "Oferta vencida" : "Oferta rechazada") + currentUser + (reason == null || reason.isEmpty() ? "" : ": " + reason), clock.now());
        }
    }

    private void voidOffers(Delivery s, String reason) {
        for (Offer o : offersOf(s)) closeOffer(o, OfferStatus.VOIDED, reason);
    }

    // =====================================================================================
    // Elegibilidad (RN-03): cuenta activa y vehículo registrado
    // =====================================================================================

    private void refreshEligibility(Courier d) {
        AccountStatus account = db.users().findById(d.getId()).map(UserAccount::getAccountStatus).orElse(AccountStatus.INACTIVE);
        accountOf.put(d.getId(), account);
        Vehicle v = db.vehicles().findByCourierIdAndActiveTrue(d.getId()).stream().findFirst().orElse(null);
        if (v == null) vehicleOf.remove(d.getId()); else vehicleOf.put(d.getId(), v);
        String reason = null;
        if (account != AccountStatus.ACTIVE) reason = "Cuenta " + account.dbCode().toLowerCase();
        else if (v == null) reason = "Sin vehículo registrado";
        d.eligibility(reason == null, reason, v == null ? 0 : v.getCapacityUnits());
    }

    // =====================================================================================
    // Vistas
    // =====================================================================================

    /** Todo lo que pintan las pantallas de la central. Se calcula una vez por cambio y se reutiliza. */
    public synchronized CentralView view(int pendingAccounts) {
        if (cachedView != null && cachedVersion == version && cachedView.pendingAccounts() == pendingAccounts
                && clock.now() - cachedView.time() < 2.0 / 60) return cachedView;
        double now = clock.now();
        Environment e = environment(distances());
        Map<Long, Schedule> etaP = new HashMap<>(), etaD = new HashMap<>();
        List<CourierView> courierViews = new ArrayList<>();
        for (Courier d : couriers.values()) {
            AccountStatus account = accountOf.get(d.getId());
            if (account == AccountStatus.PENDING || account == AccountStatus.REJECTED) continue;
            List<Schedule> hs = d.getRoute().isEmpty() ? List.of() : engine.evaluator().evaluate(d, d.getRoute(), e).schedules();
            for (Schedule h : hs) (h.stop().type() == StopType.PICKUP ? etaP : etaD).put(h.stop().delivery().getId(), h);
            courierViews.add(courierView(d, hs, now));
        }
        List<DeliveryView> deliveryViews = new ArrayList<>();
        for (Delivery s : deliveries.values()) deliveryViews.add(deliveryView(s, etaP.get(s.getId()), etaD.get(s.getId()), now));
        CentralView v = new CentralView(now, config(), settings.mode().name(), engineFailure, pendingAccounts, openIncidents,
                courierViews, deliveryViews, alerts(now, courierViews, deliveryViews, pendingAccounts));
        cachedView = v;
        cachedVersion = version;
        return v;
    }

    /** Lo que ve un domiciliario en su celular. De paso cuenta como "señal" (su app sigue viva). Sin PIN de los pedidos. */
    public synchronized MyShiftView shiftOf(long courierId) {
        Courier d = couriers.get(courierId);
        if (d == null) return null;
        double now = clock.now();
        if (d.isConnected()) d.signal(now);
        Environment e = environment(distances());
        List<Schedule> hs = d.getRoute().isEmpty() ? List.of() : engine.evaluator().evaluate(d, d.getRoute(), e).schedules();
        Map<Long, Schedule> etaP = new HashMap<>(), etaD = new HashMap<>();
        for (Schedule h : hs) (h.stop().type() == StopType.PICKUP ? etaP : etaD).put(h.stop().delivery().getId(), h);
        Set<Delivery> mine = new LinkedHashSet<>();
        for (Stop p : d.getRoute()) mine.add(p.delivery());
        for (Delivery s : deliveries.values()) if (Objects.equals(s.getCourierId(), courierId)) mine.add(s);
        List<DeliveryView> deliveryViews = mine.stream().map(s -> deliveryView(s, etaP.get(s.getId()), etaD.get(s.getId()), now).withoutPin()).toList();

        CourierOfferView offerView = null;
        Offer o = offerOf(d);
        if (o != null) {
            Delivery s = deliveries.get(o.getDeliveryId());
            double[] info = offerInfo.getOrDefault(o.getId(), new double[]{0, 0});
            if (s != null) offerView = new CourierOfferView(o.getId(), (int) Math.max(0, Math.round((o.getDeadline() - now) * 60)), settings.offerSeconds(),
                    deliveryView(s, null, null, now).withoutPin(), info[0], s.getCourierPay() == null ? 0 : s.getCourierPay(), info[1]);
        }
        List<MessageView> messages = db.messages().findTop40ByCourierIdOrderByDateDesc(courierId).stream().map(this::messageView).toList();
        List<String> templates = db.templates().findByActiveTrueOrderByIdAsc().stream()
                .filter(p -> "DOMICILIARIO".equals(p.getForRole())).map(MessageTemplate::getText).toList();
        List<String> notices = new ArrayList<>();
        if (!d.isCanReceiveOffers() && d.getIneligibleReason() != null) notices.add("No recibes ofertas: " + d.getIneligibleReason().toLowerCase());
        return new MyShiftView(now, config(), courierView(d, hs, now), deliveryViews, offerView, messages, templates, notices);
    }

    private ConfigView config() {
        return new ConfigView(area.getCenter(), area.getSouthWest(), area.getNorthEast(), settings.kmPerMin(), settings.detourFactor(),
                settings.gpsIntervalSec(), (int) Math.round(settings.arrivalMeters()),
                settings.requirePin(), settings.requirePhoto(), settings.requireSignature(), settings.offerSeconds(),
                settings.text("mapa.teselas-url"), settings.text("mapa.nominatim-url"), settings.text("ruteo.osrm-url"));
    }

    private CourierView courierView(Courier d, List<Schedule> hs, double now) {
        List<StopView> route = new ArrayList<>();
        for (Schedule h : hs) {
            DeliveryStop ps = h.stop().delivery().stop(h.stop().type());
            route.add(new StopView(h.stop().delivery().getId(), h.stop().type().name(),
                    ps == null ? "PENDING" : ps.getStatus().name(), h.arrives(), h.sale()));
        }
        Stop current = d.nextStop();
        Integer meters = current == null || d.getPosition() == null ? null : (int) Math.round(d.getPosition().distanceM(current.destination()));
        boolean noSignal = d.isConnected() && now - d.getLastSignal() > settings.minutesWithoutSignal();
        int load = 0;
        for (Stop p : d.getRoute()) if (p.type() == StopType.DROPOFF && p.delivery().getPickedUp() != null) load += p.delivery().getUnits();
        Vehicle v = vehicleOf.get(d.getId());
        Shift t = shifts.get(d.getId());
        Offer o = offerOf(d);
        return new CourierView(d.getId(), d.getName(), d.getUsername(), d.getPhone(), d.getPosition(), d.getAccuracyM(), d.getLastGps(),
                detailStatus(d, now), d.getCourierStatus().name(), d.isConnected(), noSignal, d.getConnectedSince(),
                d.getCapacity(), load, v == null ? null : v.getType().name() + " " + v.getPlate(), d.isCanReceiveOffers(), d.getIneligibleReason(),
                current == null ? null : current.delivery().getId(), meters, route, o == null ? null : o.getDeliveryId(),
                d.getConsecutiveRejections(), t == null ? null : shiftView(t), unreadMessages.getOrDefault(d.getId(), 0));
    }

    /** Estado para mostrar: además del estado operativo, qué está haciendo ahora. */
    private String detailStatus(Courier d, double now) {
        if (!d.isConnected()) return "DISCONNECTED";
        Stop p = d.nextStop();
        if (p == null) return d.getCourierStatus() == CourierStatus.BUSY ? "PAUSED" : "FREE";
        boolean there = d.getPosition() != null && d.getPosition().distanceM(p.destination()) <= settings.arrivalMeters();
        if (p.type() == StopType.PICKUP) {
            if (!there) return "TO_PICKUP";
            return now < p.delivery().getReadyAt() ? "WAITING_ORDER" : "AT_PICKUP";
        }
        if (p.delivery().isReturnTrip()) return there ? "AT_RETURN" : "RETURNING";
        return there ? "AT_DROPOFF" : "TO_DROPOFF";
    }

    private DeliveryView deliveryView(Delivery s, Schedule etaP, Schedule etaD, double now) {
        List<OfferView> offerView = new ArrayList<>();
        for (Offer o : offersOf(s))
            offerView.add(new OfferView(o.getId(), o.getDeliveryId(), o.getCourierId(), o.getMode().name(), (int) Math.max(0, Math.round((o.getDeadline() - now) * 60))));
        Long courierId = s.getCourierId();
        return new DeliveryView(s.getId(), s.getPickup(), s.getDropoff(), s.getPickupAddress(), s.getDropoffAddress(), s.getCustomerId(),
                s.getCustomer(), s.getPhone(), s.getNote(), s.getPin(), s.getStatus().name(), s.getPriority().name(), s.getSize().name(),
                s.getUnits(), s.getPaymentMethod().name(), s.getValue(), s.getAmountReceived(), s.getCourierPay(), courierId,
                s.getAssignmentOrigin() == null ? null : s.getAssignmentOrigin().name(), s.isManualAssignment(), s.isReturnTrip(),
                s.getCreated(), s.getScheduledFor(), s.getReadyAt(), s.getPickupDeadline(), s.getDeadline(), s.getQueuedSince(), s.getAssignedAt(),
                s.getPickedUp(), s.getDelivered(), s.getClosed(), s.getCloseReason(),
                etaP == null ? null : etaP.arrives(), etaD == null ? null : etaD.arrives(),
                s.getPickupStop() == null ? null : s.getPickupStop().getStatus().name(),
                s.getDropoffStop() == null ? null : s.getDropoffStop().getStatus().name(),
                routing.estimated().km(s.getPickup(), s.getDropoff()), offerView);
    }

    private ShiftView shiftView(Shift t) {
        return new ShiftView(t.getId(), t.getStart(), t.getEnd(), t.getCompleted(), t.getCollected(), t.getCollectedHandedOver(), t.getPending(), t.getEarnings());
    }

    private MessageView messageView(ChatMessage m) {
        return new MessageView(m.getId(), m.getCourierId(), m.isFromCentral(), m.getAuthor(), m.getText(), m.getDate(), m.isRead());
    }

    /** Alertas de la central (RF-M4-04): lo que hay que atender ya, de lo más urgente a lo menos. */
    private List<Alert> alerts(double now, List<CourierView> courierViews, List<DeliveryView> deliveryViews, int pendingAccounts) {
        List<Alert> list = new ArrayList<>();
        long queued = deliveryViews.stream().filter(s -> s.status().equals("QUEUED")).count();
        boolean someone = courierViews.stream().anyMatch(CourierView::connected);
        if (engineFailure != null) list.add(new Alert("engine", 3, "ENGINE_FAILURE", engineFailure, "/central"));
        if (queued > 0 && !someone) list.add(new Alert("nobody", 3, "NOBODY_CONNECTED", queued + " servicio(s) sin asignar y ningún domiciliario disponible", "/central"));
        else if (queued > 0 && settings.mode() == AssignmentMode.MANUAL)
            list.add(new Alert("manual", 2, "MANUAL_MODE", "Modo manual: " + queued + " servicio(s) esperan que los asignes", "/central"));
        double unassignedMin = settings.number("alerta.minutos-sin-asignar");
        Map<Long, String> names = courierViews.stream().collect(Collectors.toMap(CourierView::id, CourierView::name));
        for (DeliveryView s : deliveryViews) {
            if (!DeliveryStatus.valueOf(s.status()).isOpen() || s.status().equals("SCHEDULED")) continue;
            String currentUser = s.courierId() != null && names.containsKey(s.courierId()) ? " · " + names.get(s.courierId()) : "";
            if (now > s.deadline()) list.add(new Alert("late-" + s.id(), 3, "LATE", "#" + s.id() + " va " + duration(now - s.deadline()) + " tarde (límite " + hhmm(s.deadline()) + ")" + currentUser, "/central"));
            else if (s.etaDropoff() != null && s.etaDropoff() > s.deadline() + 0.5)
                list.add(new Alert("risk-" + s.id(), 2, "SLA_RISK", "#" + s.id() + " llegaría unos " + duration(s.etaDropoff() - s.deadline()) + " tarde" + currentUser, "/central"));
            if (s.pickedUp() == null && now > s.pickupDeadline())
                list.add(new Alert("pickup-" + s.id(), 3, "PICKUP_OVERDUE", "#" + s.id() + " pasó el tiempo máximo de recogida (" + hhmm(s.pickupDeadline()) + "): prioridad máxima" + currentUser, "/central"));
            if (s.status().equals("QUEUED") && s.queuedSince() != null && now - s.queuedSince() >= unassignedMin)
                list.add(new Alert("queue-" + s.id(), 2, "UNASSIGNED", "#" + s.id() + " lleva " + duration(now - s.queuedSince()) + " sin domiciliario", "/central"));
        }
        int maxRejections = settings.rejectionsAlert();
        for (CourierView d : courierViews) {
            if (d.noSignal()) list.add(new Alert("gps-" + d.id(), 2, "NO_GPS", d.name() + " está trabajando pero su celular no reporta señal", "/central/couriers"));
            if (d.connected() && d.consecutiveRejections() >= maxRejections)
                list.add(new Alert("rejections-" + d.id(), 2, "REJECTIONS", d.name() + " lleva " + d.consecutiveRejections() + " ofertas rechazadas o vencidas seguidas", "/central/couriers"));
            if (d.connected() && d.vehicle() == null)
                list.add(new Alert("vehicle-" + d.id(), 2, "NO_VEHICLE", d.name() + " está trabajando sin vehículo registrado: no recibe ofertas", "/central/accounts"));
            if (d.unreadMessages() > 0)
                list.add(new Alert("msg-" + d.id(), 1, "MESSAGE", d.name() + " te escribió (" + d.unreadMessages() + " sin leer)", "/central/couriers"));
        }
        if (openIncidents > 0) list.add(new Alert("incidents", 2, "INCIDENT", openIncidents + " novedad(es) sin cerrar", "/central/incidents"));
        if (pendingAccounts > 0) list.add(new Alert("accounts", 1, "ACCOUNTS", pendingAccounts + " domiciliario(s) esperan aprobación", "/central/accounts"));
        list.sort(Comparator.comparingInt(Alert::level).reversed());
        return list;
    }

    // =====================================================================================
    // Consultas para otras pantallas
    // =====================================================================================

    public synchronized Delivery deliveryInMemory(long id) { return deliveries.get(id); }
    public synchronized List<Courier> couriers() { return new ArrayList<>(couriers.values()); }
    public synchronized Map<Long, Vehicle> vehicles() { return new HashMap<>(vehicleOf); }
    public synchronized AssignmentMode mode() { return settings.mode(); }
    public String routingEngineStatus() { return routing.status(); }
    public double now() { return clock.now(); }

    // =====================================================================================
    // Internos
    // =====================================================================================

    /** Cambia el estado del servicio, lo guarda y deja el rastro en el historial (RN-01, RN-11). */
    private void change(Delivery s, DeliveryStatus newStatus, String username, String origin, String reason, double time) {
        DeliveryStatus previous;
        try { previous = s.changeStatus(newStatus, time); }
        catch (IllegalStateException e) { throw new IllegalArgumentException(e.getMessage() + " (servicio #" + s.getId() + ")"); }
        if (newStatus == DeliveryStatus.QUEUED) { s.getPickupStop().release(); s.getDropoffStop().release(); saveStops(s); }
        db.deliveries().save(s);
        db.history().save(new StatusChange(s.getId(), previous, newStatus, time, username, origin, reason));
    }

    private void audit(String username, String action, String entity, Long id, String detail, boolean manual) {
        db.audit().save(new AuditEntry(clock.now(), username == null ? "sistema" : username, action, entity, id,
                detail == null ? null : (detail.length() > 500 ? detail.substring(0, 500) : detail), manual));
    }

    private void notifyCourier(Courier d, String type, Map<String, Object> data) {
        notifier.toCourier(d.getUsername(), type, data);
    }

    private void changed() { version++; notifier.changed(); }

    private void saveAddress(long customerId, String text, GeoPoint point) {
        boolean exists = db.addresses().findByCustomerIdAndActiveTrue(customerId).stream()
                .anyMatch(x -> x.getText().equalsIgnoreCase(text) || x.point().distanceM(point) < 25);
        if (!exists) db.addresses().save(new Address(customerId, text, null, point));
    }

    private Delivery opened(long id) {
        Delivery s = deliveries.get(id);
        if (s == null) throw new IllegalArgumentException("No existe el servicio #" + id + " en la operación actual");
        if (!s.isOpen()) throw new IllegalArgumentException("El servicio #" + id + " ya terminó (" + DeliveryStateMachine.text(s.getStatus()) + ")");
        return s;
    }

    private Courier courier(long id) {
        Courier d = couriers.get(id);
        if (d == null) throw new IllegalArgumentException("Ese domiciliario no existe");
        return d;
    }

    private Courier courierOf(Delivery s) {
        return s.getCourierId() == null ? null : couriers.get(s.getCourierId());
    }

    private void validatePoint(GeoPoint p, String what) {
        if (p == null || !p.isValid()) throw new IllegalArgumentException("Falta el punto de " + what + " en el mapa");
        if (!area.contains(p)) throw new IllegalArgumentException("El punto de " + what + " está fuera del área de operación");
    }

    private static String requireReason(String reason, String target) {
        String m = clear(reason, 300);
        if (m.length() < 3) throw new IllegalArgumentException("Escribe el motivo para " + target);
        return m;
    }

    /** Valida una imagen en base64 (foto o firma). */
    private static String image(String item, String what) {
        if (!item.startsWith("data:image/")) throw new IllegalArgumentException("La " + what + " no es una imagen válida");
        if (item.length() > MAX_IMAGE) throw new IllegalArgumentException("La " + what + " es muy pesada");
        return item;
    }

    static String clear(String text, int max) {
        String t = text == null ? "" : text.trim().replaceAll("\\s+", " ");
        return t.length() > max ? t.substring(0, max) : t;
    }

    private static String cleanPhone(String t) { return clear(t, 20); }

    static String hhmm(double minutes) {
        return HH_MM.format(Instant.ofEpochMilli(Math.round(minutes * 60000)).atZone(BOGOTA));
    }

    static String duration(double minutes) {
        long m = Math.max(0, Math.round(minutes));
        return m < 60 ? m + " min" : (m / 60) + " h " + String.format("%02d", m % 60) + " min";
    }
}
