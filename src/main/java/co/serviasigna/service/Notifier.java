package co.serviasigna.service;

import java.util.Map;

/**
 * Avisos en tiempo real (RF-M9-01, RF-M9-02). La implementación real los manda por WebSocket STOMP
 * (tiemporeal/StompNotifier); en las pruebas no hace nada.
 */
public interface Notifier {
    /** Algo cambió en la operación: las pantallas de la central deben refrescarse. */
    void changed();

    /** Aviso para un domiciliario: OFERTA, OFERTA_CERRADA, ASIGNADO, REVOCADO, CANCELADO, MENSAJE, DEVOLUCION. */
    void toCourier(String username, String type, Map<String, Object> data);

    /** Aviso para la central: MENSAJE, NOVEDAD, MOTOR. */
    void toCentral(String type, Map<String, Object> data);

    Notifier NONE = new Notifier() {
        public void changed() {}
        public void toCourier(String username, String type, Map<String, Object> data) {}
        public void toCentral(String type, Map<String, Object> data) {}
    };
}
