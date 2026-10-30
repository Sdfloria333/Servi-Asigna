package co.serviasigna.service.view;

/** Resumen del turno (RF-M6-12): completados, cobrado, entregado a la central, pendiente y ganancias en COP. */
public record ShiftView(long id, double start, Double end, int completed, long collected, long collectedHandedOver,
                         long pending, long earnings) {}
