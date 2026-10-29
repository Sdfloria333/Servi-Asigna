package co.serviasigna.service.view;

import co.serviasigna.domain.GeoPoint;
import java.util.List;

/**
 * Un domiciliario para la pantalla.
 * courierStatus: FUERA_DE_SERVICIO, DISPONIBLE, OCUPADO, EN_RUTA (sección 6.3).
 * estado: detalle para mostrar (LIBRE, PAUSA, HACIA_RECOGIDA, ESPERANDO_PEDIDO, EN_RECOGIDA, HACIA_ENTREGA, EN_ENTREGA, DEVOLVIENDO, DESCONECTADO).
 * elegible / ineligibleReason: si puede recibir ofertas (RN-03) y por qué no.
 */
public record CourierView(long id, String name, String username, String phone, GeoPoint position, Double accuracyM, Double lastGps,
                        String status, String courierStatus, boolean connected, boolean noSignal, Double connectedSince,
                        int capacity, int currentLoad, String vehicle, boolean eligible, String ineligibleReason,
                        Long currentDelivery, Integer metersToNext, List<StopView> route, Long offerDeliveryId,
                        int consecutiveRejections, ShiftView shift, int unreadMessages) {}
