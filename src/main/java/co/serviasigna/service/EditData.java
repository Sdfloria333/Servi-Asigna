package co.serviasigna.service;

import co.serviasigna.domain.PaymentMethod;
import co.serviasigna.domain.Priority;
import co.serviasigna.domain.PackageSize;

/** Edición de un servicio que aún no se recoge (RF-M3-03). readyAt / limite en minutos de reloj. */
public record EditData(String note, String customer, String phone, PackageSize size, Priority priority,
                           PaymentMethod paymentMethod, Long value, Double readyAt, Double deadline) {}
