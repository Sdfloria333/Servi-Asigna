package co.serviasigna.service;

/**
 * "Recogí" / "Entregué" desde la app. Al entregar: pin, foto y firma son las pruebas (RF-M6-06);
 * amountReceived es lo cobrado si es contra entrega (RN-10).
 * Sin conexión (RF-M6-11) la app guarda el evento y lo manda después con su hora real (eventTime),
 * la posición de ese momento (lat/lng) y un eventId para no procesarlo dos veces.
 */
public record Confirmation(String pin, String photo, String signature, Long amountReceived,
                           Double lat, Double lng, Double eventTime, String eventId) {
    public static Confirmation simple() { return new Confirmation(null, null, null, null, null, null, null, null); }
    public static Confirmation withPin(String pin) { return new Confirmation(pin, null, null, null, null, null, null, null); }
}
