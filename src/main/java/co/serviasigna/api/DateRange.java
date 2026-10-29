package co.serviasigna.api;

import java.time.LocalDate;
import java.time.ZoneId;

/** Convierte fechas de la pantalla (2026-10-26) a minutos de reloj, con la hora de Colombia. */
public final class DateRange {
    public static final ZoneId ZONE = ZoneId.of("America/Bogota");
    private DateRange() {}

    public record Minutes(double from, double to) {}

    /** desde = inicio de ese día; hasta = fin del día "hasta". Por defecto: hoy. Máximo 366 días. */
    public static Minutes of(String from, String to) {
        LocalDate today = LocalDate.now(ZONE);
        LocalDate d = from == null || from.isBlank() ? today : LocalDate.parse(from);
        LocalDate h = to == null || to.isBlank() ? d : LocalDate.parse(to);
        if (h.isBefore(d)) throw new IllegalArgumentException("La fecha final es anterior a la inicial");
        if (h.isAfter(d.plusDays(366))) throw new IllegalArgumentException("El rango máximo es de un año");
        return new Minutes(minutes(d), minutes(h.plusDays(1)) - 1.0 / 60);
    }

    static double minutes(LocalDate day) { return day.atStartOfDay(ZONE).toInstant().toEpochMilli() / 60000.0; }
}
