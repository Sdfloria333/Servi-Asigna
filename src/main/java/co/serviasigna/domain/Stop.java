package co.serviasigna.domain;

/**
 * Un paso de la ruta de un domiciliario, el que usa el motor: recoger o entregar un servicio.
 * <p>
 * Si el servicio se canceló con el pedido a bordo (RN-07, devolución), la "entrega" es volver al punto de recogida.
 * <p>
 * EN: A step in a courier route, the one used by the engine: pick up or drop off a delivery.
 * <p>
 * If the delivery was cancelled with the order on board (RN-07, return), the "drop-off" is going back to the pickup
 * point.
 *
 * @param delivery el servicio / the delivery
 * @param type recoger o entregar / pickup or drop-off
 */
public record Stop(Delivery delivery, StopType type) {
    /**
     * Devuelve a dónde tiene que ir el domiciliario en este paso.
     * <p>
     * EN: Returns where the courier has to go in this step.
     *
     * @return el punto de destino / the destination point
     */
    public GeoPoint destination() {
        if (type == StopType.PICKUP || delivery.isReturnTrip()) return delivery.getPickup();
        return delivery.getDropoff();
    }
}
