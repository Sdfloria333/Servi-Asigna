package co.serviasigna.service.view;

/** Una oferta pendiente: a quién y cuántos segundos le quedan (RN-05). */
public record OfferView(long id, long deliveryId, long courierId, String mode, int secondsLeft) {}
