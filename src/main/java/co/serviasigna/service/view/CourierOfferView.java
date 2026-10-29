package co.serviasigna.service.view;

/** La oferta que ve el domiciliario: el servicio, cuánto le queda para responder, cuánto se desvía y cuánto gana. */
public record CourierOfferView(long id, int secondsLeft, double totalSeconds, DeliveryView delivery,
                              double kmExtra, long payment, double etaDropoff) {}
