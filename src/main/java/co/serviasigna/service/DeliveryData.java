package co.serviasigna.service;

import co.serviasigna.domain.GeoPoint;
import co.serviasigna.domain.PackageSize;
import co.serviasigna.domain.PaymentMethod;
import co.serviasigna.domain.Priority;

/**
 * Lo que la central escribe para crear un servicio (RF-M3-01). Lo opcional puede venir en null.
 * readyMin / deadlineMin / maxPickupMin = minutos desde ahora (o desde scheduledFor si es programado).
 * scheduledFor = minutos de reloj en que el servicio debe entrar a la cola (RF-M3-08), o null.
 * valor = null para calcularlo con la tarifa vigente (RF-M3-10).
 */
public record DeliveryData(GeoPoint pickup, String pickupAddress, GeoPoint dropoff, String dropoffAddress,
                            Long customerId, String customer, String phone, String note,
                            PackageSize size, Priority priority, PaymentMethod paymentMethod, Long value,
                            Double readyMin, Double deadlineMin, Double maxPickupMin, Double scheduledFor,
                            Boolean saveCustomer) {}
