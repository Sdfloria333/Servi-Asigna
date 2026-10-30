package co.serviasigna.service;

import co.serviasigna.domain.*;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Consultas de solo lectura sobre lo guardado en Oracle: búsqueda histórica de servicios (RF-M3-06),
 * detalle con línea de tiempo, ofertas, evidencias y decisiones del motor (RF-M3-07, RF-M5-07),
 * novedades (RF-M7-04), recorrido GPS (RF-M4-03) y auditoría (RF-M10-05).
 */
@Service
public class QueryService {
    private static final int MAXIMUM = 500;
    private final DataStore db;
    private final DispatchService dispatch;

    public QueryService(DataStore db, DispatchService dispatch) { this.db = db; this.dispatch = dispatch; }

    public record DeliveryRow(long id, String status, double created, String customer, String phone, String pickupAddress,
                               String dropoffAddress, Long courierId, String courier, long value, String paymentMethod, String priority,
                               Double delivered, Double closed, boolean onTime, String closeReason) {}

    public record TimelineEvent(double date, String title, String detail, String username) {}

    public record Detail(Map<String, Object> delivery, List<TimelineEvent> timeline, List<Map<String, Object>> offers,
                          List<Map<String, Object>> proofs, List<Map<String, Object>> decisions,
                          List<Map<String, Object>> incidents, List<GeoPoint> track) {}

    public record IncidentReport(long id, String type, String typeText, Long deliveryId, Long courierId, String courier, String reportedBy,
                          String description, boolean hasPhoto, String status, String decision, String resolution,
                          double opened, Double closed, String closedBy, List<IncidentFollowUp> followUp) {}

    /** Servicios creados entre dos horas (minutos de reloj), filtrando por estado y por texto (cliente, teléfono, dirección, #id). */
    public List<DeliveryRow> search(double from, double to, String status, String text, Long courierId) {
        Map<Long, String> names = names();
        String t = text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
        return db.deliveries().findByCreatedBetweenOrderByIdDesc(from, to).stream()
                .filter(s -> status == null || status.isBlank() || s.getStatus().name().equals(status))
                .filter(s -> courierId == null || courierId.equals(s.getCourierId()))
                .filter(s -> t.isEmpty() || contains(s, t))
                .limit(MAXIMUM)
                .map(s -> row(s, names))
                .toList();
    }

    private static boolean contains(Delivery s, String t) {
        if (t.startsWith("#")) return String.valueOf(s.getId()).equals(t.substring(1));
        return String.valueOf(s.getId()).equals(t)
                || (s.getCustomer() != null && s.getCustomer().toLowerCase(Locale.ROOT).contains(t))
                || (s.getPhone() != null && s.getPhone().contains(t))
                || (s.getPickupAddress() != null && s.getPickupAddress().toLowerCase(Locale.ROOT).contains(t))
                || (s.getDropoffAddress() != null && s.getDropoffAddress().toLowerCase(Locale.ROOT).contains(t))
                || (s.getNote() != null && s.getNote().toLowerCase(Locale.ROOT).contains(t));
    }

    private DeliveryRow row(Delivery s, Map<Long, String> names) {
        return new DeliveryRow(s.getId(), s.getStatus().name(), s.getCreated(), s.getCustomer(), s.getPhone(), s.getPickupAddress(),
                s.getDropoffAddress(), s.getCourierId(), names.get(s.getCourierId()), s.getValue(), s.getPaymentMethod().name(),
                s.getPriority().name(), s.getDelivered(), s.getClosed(),
                s.getDelivered() != null && s.getDelivered() <= s.getDeadline(), s.getCloseReason());
    }

    /** Todo lo que pasó con un servicio. */
    public Detail detail(long id) {
        Delivery s = Optional.ofNullable(dispatch.deliveryInMemory(id)).or(() -> db.deliveries().findById(id))
                .orElseThrow(() -> new NoSuchElementException("No existe el servicio #" + id));
        Map<Long, String> names = names();

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", s.getId()); m.put("status", s.getStatus().name()); m.put("statusText", s.getStatus().dbCode().replace('_', ' '));
        m.put("pickup", s.getPickup()); m.put("dropoff", s.getDropoff());
        m.put("pickupAddress", s.getPickupAddress()); m.put("dropoffAddress", s.getDropoffAddress());
        m.put("customerId", s.getCustomerId()); m.put("customer", s.getCustomer()); m.put("phone", s.getPhone()); m.put("note", s.getNote());
        m.put("size", s.getSize().name()); m.put("priority", s.getPriority().name()); m.put("paymentMethod", s.getPaymentMethod().name());
        m.put("value", s.getValue()); m.put("amountReceived", s.getAmountReceived()); m.put("courierPay", s.getCourierPay());
        m.put("pin", s.getPin());
        m.put("created", s.getCreated()); m.put("scheduledFor", s.getScheduledFor()); m.put("readyAt", s.getReadyAt());
        m.put("pickupDeadline", s.getPickupDeadline()); m.put("deadline", s.getDeadline());
        m.put("assignedAt", s.getAssignedAt()); m.put("pickedUp", s.getPickedUp()); m.put("delivered", s.getDelivered()); m.put("closed", s.getClosed());
        m.put("courierId", s.getCourierId()); m.put("courier", names.get(s.getCourierId()));
        m.put("origin", s.getAssignmentOrigin() == null ? null : s.getAssignmentOrigin().name());
        m.put("manual", s.isManualAssignment()); m.put("returnTrip", s.isReturnTrip());
        m.put("closeReason", s.getCloseReason()); m.put("createdBy", s.getCreatedBy());
        m.put("opened", s.getStatus().isOpen());

        List<TimelineEvent> line = new ArrayList<>();
        for (StatusChange h : db.history().findByDeliveryIdOrderByDateAscIdAsc(id)) {
            String title = h.getPreviousStatus() == null ? "Creado como " + text(h.getNewStatus()) : text(h.getPreviousStatus()) + " → " + text(h.getNewStatus());
            String det = (h.getOrigin() == null ? "" : h.getOrigin().toLowerCase(Locale.ROOT)) + (h.getReason() == null ? "" : " · " + h.getReason());
            line.add(new TimelineEvent(h.getDate(), title, det.trim(), h.getUsername()));
        }
        for (DeliveryStop p : db.stops().findByDeliveryId(id)) {
            String what = p.getType() == StopType.PICKUP ? "recogida" : "entrega";
            if (p.getArrivalTime() != null) line.add(new TimelineEvent(p.getArrivalTime(), "Llegó al punto de " + what, "", names.get(p.getCourierId())));
        }
        List<Map<String, Object>> offers = new ArrayList<>();
        for (Offer o : db.offers().findByDeliveryIdOrderBySentAsc(id)) {
            Map<String, Object> x = new LinkedHashMap<>();
            x.put("id", o.getId()); x.put("courierId", o.getCourierId()); x.put("courier", names.get(o.getCourierId()));
            x.put("mode", o.getMode().name()); x.put("status", o.getStatus().name()); x.put("sent", o.getSent());
            x.put("answered", o.getAnsweredAt()); x.put("cost", o.getCost()); x.put("reason", o.getRejectionReason());
            offers.add(x);
            line.add(new TimelineEvent(o.getSent(), "Ofertado a " + names.getOrDefault(o.getCourierId(), "#" + o.getCourierId()),
                    o.getMode().dbCode().toLowerCase(Locale.ROOT) + " · " + o.getStatus().dbCode().toLowerCase(Locale.ROOT)
                            + (o.getRejectionReason() == null ? "" : " (" + o.getRejectionReason() + ")"), null));
        }
        List<Map<String, Object>> proofs = new ArrayList<>();
        for (DeliveryProof e : db.proofs().findByDeliveryId(id)) {
            Map<String, Object> x = new LinkedHashMap<>();
            x.put("type", e.getType().name()); x.put("date", e.getDate());
            x.put("item", e.getType() == ProofType.PIN ? "correcto" : e.getItem());
            proofs.add(x);
        }
        List<Map<String, Object>> decisions = new ArrayList<>();
        for (EngineDecision d : db.decisions().findByDeliveryIdOrderByDateAsc(id)) {
            Map<String, Object> x = new LinkedHashMap<>();
            x.put("date", d.getDate()); x.put("mode", d.getMode().name()); x.put("chosenId", d.getChosenId());
            x.put("chosen", names.get(d.getChosenId())); x.put("cost", d.getCost()); x.put("candidates", d.getCandidates());
            decisions.add(x);
        }
        List<Map<String, Object>> incidents = new ArrayList<>();
        for (Incident i : db.incidents().findByDeliveryIdOrderByIdAsc(id)) {
            Map<String, Object> x = new LinkedHashMap<>();
            x.put("id", i.getId()); x.put("type", i.getType().text()); x.put("description", i.getDescription());
            x.put("status", i.getStatus().name()); x.put("decision", i.getDecisionData() == null ? null : i.getDecisionData().name());
            x.put("opened", i.getOpened());
            incidents.add(x);
            line.add(new TimelineEvent(i.getOpened(), "Novedad: " + i.getType().text(), i.getDescription(), i.getReportedBy()));
        }
        line.sort(Comparator.comparingDouble(TimelineEvent::date));

        List<GeoPoint> track = List.of();
        if (s.getCourierId() != null && s.getAssignedAt() != null) {
            double end = s.getClosed() != null ? s.getClosed() : (s.getDelivered() != null ? s.getDelivered() : dispatch.now());
            track = track(s.getCourierId(), s.getAssignedAt(), end);
        }
        return new Detail(m, line, offers, proofs, decisions, incidents, track);
    }

    private static String text(DeliveryStatus e) { return e.name().replace('_', ' ').toLowerCase(Locale.ROOT); }

    /** Puntos GPS que mandó un domiciliario entre dos horas (para dibujar por dónde anduvo). */
    public List<GeoPoint> track(long courierId, double from, double to) {
        return db.locations().findByCapturedAtBetweenOrderByCourierIdAscCapturedAtAsc(from, to).stream()
                .filter(u -> u.getCourierId() == courierId).map(GpsLocation::point).limit(5000).toList();
    }

    /** Novedades abiertas o en atención (abiertas=true) o todas las de un rango. */
    public List<IncidentReport> incidents(boolean open, double from, double to) {
        Map<Long, String> names = names();
        List<Incident> list = open ? db.incidents().findByStatusNotOrderByIdDesc(IncidentStatus.CLOSED)
                : db.incidents().findByOpenedBetweenOrderByIdDesc(from, to);
        return list.stream().limit(MAXIMUM).map(i -> new IncidentReport(i.getId(), i.getType().name(), i.getType().text(), i.getDeliveryId(),
                i.getCourierId(), names.get(i.getCourierId()), i.getReportedBy(), i.getDescription(),
                i.getPhoto() != null && !i.getPhoto().isBlank(), i.getStatus().name(),
                i.getDecisionData() == null ? null : i.getDecisionData().name(), i.getResolution(), i.getOpened(), i.getClosed(), i.getClosedBy(),
                db.followUps().findByIncidentIdOrderByDateAsc(i.getId()))).toList();
    }

    public String incidentPhoto(long id) {
        return db.incidents().findById(id).map(Incident::getPhoto).orElse(null);
    }

    public List<AuditEntry> audit(double from, double to) {
        return db.audit().findByDateBetweenOrderByDateDesc(from, to).stream().limit(MAXIMUM).toList();
    }

    /** Servicios de un cliente (para el detalle del cliente). */
    public List<DeliveryRow> byCustomer(long customerId) {
        Map<Long, String> names = names();
        return db.deliveries().findByCustomerIdOrderByIdDesc(customerId).stream().limit(100).map(s -> row(s, names)).toList();
    }

    public List<Shift> shifts(double from, double to) { return db.shifts().findByStartBetween(from, to); }

    private Map<Long, String> names() {
        return dispatch.couriers().stream().collect(Collectors.toMap(Courier::getId, Courier::getName, (a, b) -> a));
    }
}
