package co.serviasigna.service.view;

import co.serviasigna.domain.GeoPoint;
import java.util.List;

/**
 * Un servicio para la pantalla. Horas en minutos de reloj; las "eta" las estima el motor (null si no aplica).
 * El PIN solo lo ve la central: al domiciliario se le manda en null (él se lo pide al cliente).
 */
public record DeliveryView(long id, GeoPoint pickup, GeoPoint dropoff, String pickupAddress, String dropoffAddress,
                            Long customerId, String customer, String phone, String note, String pin,
                            String status, String priority, String size, int units, String paymentMethod, long value,
                            Long amountReceived, Long courierPay, Long courierId, String origin, boolean manual, boolean returnTrip,
                            double created, Double scheduledFor, double readyAt, double pickupDeadline, double deadline,
                            Double queuedSince, Double assignedAt, Double pickedUp, Double delivered, Double closed, String closeReason,
                            Double etaPickup, Double etaDropoff, String pickupStatus, String dropoffStatus,
                            double deliveryKm, List<OfferView> offers) {
    public DeliveryView withoutPin() {
        return new DeliveryView(id, pickup, dropoff, pickupAddress, dropoffAddress, customerId, customer, phone, note, null,
                status, priority, size, units, paymentMethod, value, amountReceived, courierPay, courierId, origin, manual, returnTrip,
                created, scheduledFor, readyAt, pickupDeadline, deadline, queuedSince, assignedAt, pickedUp, delivered, closed, closeReason,
                etaPickup, etaDropoff, pickupStatus, dropoffStatus, deliveryKm, offers);
    }
}
