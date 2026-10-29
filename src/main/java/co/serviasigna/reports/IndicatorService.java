package co.serviasigna.reports;

import co.serviasigna.domain.*;
import co.serviasigna.service.DataStore;
import co.serviasigna.service.Settings;
import co.serviasigna.service.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Indicadores de éxito de la sección 3.3 (I1–I7) por rango de fechas (RF-M10-01), comparados con la línea base
 * (RF-M10-07), más el flujo por hora/día y el desempeño por domiciliario (RF-M10-03). Todo sale de Oracle.
 */
@Service
public class IndicatorService {
    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");

    private final DataStore db;
    private final Settings settings;
    private final Clock clock;

    public IndicatorService(DataStore db, Settings settings, Clock clock) { this.db = db; this.settings = settings; this.clock = clock; }

    /** Un indicador: valor, unidad, línea base, si "más es mejor" y la variación frente a la línea base. */
    public record Indicator(String code, String name, Double value, String unit, Double baseline, boolean higherIsBetter,
                            Double variationPct, Boolean improves, String formula, int samples) {}
    public record GeoPoint(String label, int created, int delivered) {}
    public record Performance(long courierId, String name, int delivered, int onTime, Double onTimePct, Double averageMinutes,
                            int offers, int accepted, Double acceptancePct, double km, long earnings, long collected, long collectedPending) {}
    public record Report(double from, double to, List<Indicator> indicators, Map<String, Number> figures,
                          List<GeoPoint> flow, List<Performance> couriers) {}

    public Report calculate(double from, double to) {
        if (to <= from) throw new IllegalArgumentException("El rango de fechas no es válido");
        if (to - from > 400 * 1440) throw new IllegalArgumentException("El rango no puede pasar de 400 días");
        List<Delivery> deliveries = db.deliveries().findByCreatedBetweenOrderByIdDesc(from, to);
        List<Offer> offers = db.offers().findBySentBetween(from, to);
        List<Shift> shifts = db.shifts().findByStartBetween(from - 1440, to);
        long interventions = db.audit().countByManualInterventionTrueAndDateBetween(from, to);

        List<Delivery> delivered = deliveries.stream().filter(s -> s.getStatus() == DeliveryStatus.DELIVERED).toList();
        List<Delivery> closed = deliveries.stream().filter(s -> s.getStatus() == DeliveryStatus.DELIVERED || s.getStatus() == DeliveryStatus.FAILED).toList();
        List<Delivery> assigned = deliveries.stream().filter(s -> s.getAssignedAt() != null).toList();
        List<Delivery> withOrigin = deliveries.stream().filter(s -> s.getAssignmentOrigin() != null).toList();
        List<Offer> answered = offers.stream().filter(o -> o.getStatus() == OfferStatus.ACCEPTED
                || o.getStatus() == OfferStatus.REJECTED || o.getStatus() == OfferStatus.EXPIRED).toList();
        double km = shifts.stream().mapToDouble(Shift::getKmTravelled).sum();
        Set<Long> hours = new HashSet<>();
        for (Delivery s : deliveries) hours.add((long) Math.floor(s.getCreated() / 60));
        double operatingHours = Math.max(hours.size(), deliveries.isEmpty() ? 0 : 1);

        List<Indicator> ind = new ArrayList<>();
        ind.add(indicator("I1", "Tiempo de asignación", average(assigned, s -> s.getAssignedAt() - base(s)), "min",
                "linea-base.tiempo-asignacion-min", false, "Promedio de (hora de aceptación − hora en que el servicio entró a operar)", assigned.size()));
        ind.add(indicator("I2", "Tiempo de entrega", average(delivered, s -> s.getDelivered() - base(s)), "min",
                "linea-base.tiempo-entrega-min", false, "Promedio de (hora de entrega − hora de creación)", delivered.size()));
        ind.add(indicator("I3", "Asignación sin intervención de la central", pct(withOrigin.stream().filter(s -> s.getAssignmentOrigin() != AssignmentOrigin.MANUAL).count(), withOrigin.size()), "%",
                null, true, "Servicios asignados por el motor (automático o difusión) / servicios asignados × 100", withOrigin.size()));
        ind.add(indicator("I4", "Distancia por servicio", delivered.isEmpty() ? null : km / delivered.size(), "km",
                "linea-base.km-por-servicio", false, "Kilómetros recorridos por los domiciliarios en sus turnos / servicios entregados", delivered.size()));
        ind.add(indicator("I5", "Tasa de aceptación de ofertas", pct(answered.stream().filter(o -> o.getStatus() == OfferStatus.ACCEPTED).count(), answered.size()), "%",
                null, true, "Ofertas aceptadas / ofertas respondidas o vencidas × 100", answered.size()));
        ind.add(indicator("I6", "Cumplimiento de SLA", pct(closed.stream().filter(this::onTime).count(), closed.size()), "%",
                "linea-base.cumplimiento-sla", true, "Servicios recogidos y entregados dentro de su límite / servicios terminados (entregados + fallidos) × 100", closed.size()));
        ind.add(indicator("I7", "Carga de la central", operatingHours == 0 ? null : interventions / operatingHours, "interv./h",
                "linea-base.intervenciones-hora", false, "Intervenciones manuales (asignar, reasignar, revocar, cancelar, editar, cerrar novedades) / horas con operación", (int) interventions));

        Map<String, Number> figures = new LinkedHashMap<>();
        figures.put("created", deliveries.size());
        figures.put("delivered", delivered.size());
        figures.put("cancelled", deliveries.stream().filter(s -> s.getStatus() == DeliveryStatus.CANCELLED).count());
        figures.put("failed", deliveries.stream().filter(s -> s.getStatus() == DeliveryStatus.FAILED).count());
        figures.put("open", deliveries.stream().filter(Delivery::isOpen).count());
        Double waitTime = average(deliveries.stream().filter(s -> s.getPickedUp() != null).toList(), s -> s.getPickedUp() - base(s));
        figures.put("pickupWaitMin", waitTime == null ? 0 : round(waitTime));
        figures.put("totalValue", delivered.stream().mapToLong(Delivery::getValue).sum());
        figures.put("collected", delivered.stream().mapToLong(s -> s.getAmountReceived() == null ? 0 : s.getAmountReceived()).sum());
        figures.put("courierPayments", delivered.stream().mapToLong(s -> s.getCourierPay() == null ? 0 : s.getCourierPay()).sum());
        figures.put("kmTravelled", round(km));
        figures.put("interventions", interventions);
        figures.put("operatingHours", operatingHours);
        figures.put("incidents", db.incidents().findByOpenedBetweenOrderByIdDesc(from, to).size());

        return new Report(from, to, ind, figures, flow(deliveries, from, to), performance(deliveries, offers, shifts));
    }

    private boolean onTime(Delivery s) {
        return s.getStatus() == DeliveryStatus.DELIVERED && s.getDelivered() <= s.getDeadline()
                && s.getPickedUp() != null && s.getPickedUp() <= s.getPickupDeadline();
    }

    /** Hora desde la que se cuenta: la de creación o, si era programado, la hora programada. */
    private static double base(Delivery s) { return s.getScheduledFor() != null ? s.getScheduledFor() : s.getCreated(); }

    private Indicator indicator(String code, String name, Double value, String unit, String baseKey, boolean higherIsBetter, String formula, int samples) {
        Double lb = baseKey == null ? null : settings.number(baseKey);
        Double variation = null; Boolean improves = null;
        if (value != null && lb != null && lb != 0) {
            variation = round((value - lb) / lb * 100);
            improves = higherIsBetter ? value >= lb : value <= lb;
        }
        return new Indicator(code, name, value == null ? null : round(value), unit, lb, higherIsBetter, variation, improves, formula, samples);
    }

    /** Creados y entregados por hora (rango de hasta 2 días) o por día. */
    private List<GeoPoint> flow(List<Delivery> deliveries, double from, double to) {
        boolean byHour = to - from <= 2 * 1440;
        DateTimeFormatter f = DateTimeFormatter.ofPattern(byHour ? "dd/MM HH'h'" : "dd/MM");
        Function<Double, String> bucketKey = m -> f.format(LocalDateTime.ofInstant(Instant.ofEpochMilli(Math.round(m * 60000)), BOGOTA));
        Map<String, int[]> map = new LinkedHashMap<>();
        double step = byHour ? 60 : 1440;
        for (double t = from; t < to && map.size() < 400; t += step) map.put(bucketKey.apply(t), new int[2]);
        for (Delivery s : deliveries) {
            int[] a = map.get(bucketKey.apply(s.getCreated())); if (a != null) a[0]++;
            if (s.getDelivered() != null) { int[] b = map.get(bucketKey.apply(s.getDelivered())); if (b != null) b[1]++; }
        }
        return map.entrySet().stream().map(e -> new GeoPoint(e.getKey(), e.getValue()[0], e.getValue()[1])).toList();
    }

    private List<Performance> performance(List<Delivery> deliveries, List<Offer> offers, List<Shift> shifts) {
        Map<Long, String> names = db.couriers().findAll().stream().collect(Collectors.toMap(Courier::getId, Courier::getName));
        Set<Long> ids = new TreeSet<>();
        deliveries.stream().map(Delivery::getCourierId).filter(Objects::nonNull).forEach(ids::add);
        offers.forEach(o -> ids.add(o.getCourierId()));
        shifts.forEach(t -> ids.add(t.getCourierId()));
        List<Performance> list = new ArrayList<>();
        for (Long id : ids) {
            List<Delivery> own = deliveries.stream().filter(s -> id.equals(s.getCourierId()) && s.getStatus() == DeliveryStatus.DELIVERED).toList();
            int onTime = (int) own.stream().filter(this::onTime).count();
            List<Offer> os = offers.stream().filter(o -> id.equals(o.getCourierId())
                    && o.getStatus() != OfferStatus.VOIDED && o.getStatus() != OfferStatus.PENDING).toList();
            int accepted = (int) os.stream().filter(o -> o.getStatus() == OfferStatus.ACCEPTED).count();
            List<Shift> ts = shifts.stream().filter(t -> id.equals(t.getCourierId())).toList();
            list.add(new Performance(id, names.getOrDefault(id, "#" + id), own.size(), onTime, pct(onTime, own.size()),
                    average(own, s -> s.getDelivered() - base(s)), os.size(), accepted, pct(accepted, os.size()),
                    round(ts.stream().mapToDouble(Shift::getKmTravelled).sum()),
                    own.stream().mapToLong(s -> s.getCourierPay() == null ? 0 : s.getCourierPay()).sum(),
                    own.stream().mapToLong(s -> s.getAmountReceived() == null ? 0 : s.getAmountReceived()).sum(),
                    ts.stream().mapToLong(Shift::getPending).sum()));
        }
        list.sort(Comparator.comparingInt(Performance::delivered).reversed());
        return list;
    }

    // ---------------- CSV (RF-M10-06) ----------------

    /** Servicios del rango en CSV (separado por ";" y con BOM para que Excel lo abra bien en español). */
    public String deliveriesCsv(double from, double to) {
        DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        Function<Double, String> date = m -> m == null ? "" : f.format(LocalDateTime.ofInstant(Instant.ofEpochMilli(Math.round(m * 60000)), BOGOTA));
        Map<Long, String> names = db.couriers().findAll().stream().collect(Collectors.toMap(Courier::getId, Courier::getName));
        StringBuilder sb = new StringBuilder("﻿id;estado;creado;asignado;recogido;entregado;limite;a_tiempo;domiciliario;origen;cliente;telefono;recogida;entrega;tamano;forma_pago;valor_cop;valor_recibido_cop;motivo\n");
        for (Delivery s : db.deliveries().findByCreatedBetweenOrderByIdDesc(from, to)) {
            sb.append(s.getId()).append(';').append(s.getStatus()).append(';').append(date.apply(s.getCreated())).append(';')
              .append(date.apply(s.getAssignedAt())).append(';').append(date.apply(s.getPickedUp())).append(';').append(date.apply(s.getDelivered())).append(';')
              .append(date.apply(s.getDeadline())).append(';').append(s.getStatus() == DeliveryStatus.DELIVERED ? (onTime(s) ? "SI" : "NO") : "").append(';')
              .append(csv(s.getCourierId() == null ? "" : names.getOrDefault(s.getCourierId(), ""))).append(';')
              .append(s.getAssignmentOrigin() == null ? "" : s.getAssignmentOrigin()).append(';').append(csv(s.getCustomer())).append(';').append(csv(s.getPhone())).append(';')
              .append(csv(s.getPickupAddress() == null || s.getPickupAddress().isEmpty() ? s.getPickup().lat() + "," + s.getPickup().lng() : s.getPickupAddress())).append(';')
              .append(csv(s.getDropoffAddress() == null || s.getDropoffAddress().isEmpty() ? s.getDropoff().lat() + "," + s.getDropoff().lng() : s.getDropoffAddress())).append(';')
              .append(s.getSize()).append(';').append(s.getPaymentMethod()).append(';').append(s.getValue()).append(';')
              .append(s.getAmountReceived() == null ? "" : s.getAmountReceived()).append(';').append(csv(s.getCloseReason())).append('\n');
        }
        return sb.toString();
    }

    public String indicatorsCsv(double from, double to) {
        Report r = calculate(from, to);
        StringBuilder sb = new StringBuilder("﻿codigo;indicador;valor;unidad;linea_base;variacion_pct;mejora;muestras;formula\n");
        for (Indicator i : r.indicators())
            sb.append(i.code()).append(';').append(csv(i.name())).append(';').append(num(i.value())).append(';').append(i.unit()).append(';')
              .append(num(i.baseline())).append(';').append(num(i.variationPct())).append(';').append(i.improves() == null ? "" : i.improves() ? "SI" : "NO").append(';')
              .append(i.samples()).append(';').append(csv(i.formula())).append('\n');
        sb.append("\ndomiciliario;entregados;a_tiempo;pct_a_tiempo;min_promedio;ofertas;aceptadas;pct_aceptacion;km;ganancias_cop;recaudo_cop;recaudo_pendiente_cop\n");
        for (Performance d : r.couriers())
            sb.append(csv(d.name())).append(';').append(d.delivered()).append(';').append(d.onTime()).append(';').append(num(d.onTimePct())).append(';')
              .append(num(d.averageMinutes())).append(';').append(d.offers()).append(';').append(d.accepted()).append(';').append(num(d.acceptancePct())).append(';')
              .append(num(d.km())).append(';').append(d.earnings()).append(';').append(d.collected()).append(';').append(d.collectedPending()).append('\n');
        return sb.toString();
    }

    public double now() { return clock.now(); }

    private static String csv(String t) {
        if (t == null) return "";
        String x = t.replace("\"", "\"\"").replace("\n", " ");
        return x.contains(";") || x.contains("\"") ? "\"" + x + "\"" : x;
    }
    private static String num(Double d) { return d == null ? "" : String.valueOf(d).replace('.', ','); }
    private static Double pct(long a, long total) { return total == 0 ? null : round(100.0 * a / total); }
    private static <T> Double average(List<T> list, java.util.function.ToDoubleFunction<T> f) {
        return list.isEmpty() ? null : list.stream().mapToDouble(f).average().orElse(0);
    }
    private static double round(double v) { return Math.round(v * 10) / 10.0; }
}
