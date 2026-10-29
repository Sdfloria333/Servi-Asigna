package co.serviasigna.service.view;

/** Una parada de la ruta: estado (sección 6.2) y hora estimada de llegada y de salida. */
public record StopView(long deliveryId, String type, String status, double arrives, double sale) {}
