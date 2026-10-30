package co.serviasigna.service.view;

import java.util.List;

/** Todo lo que necesitan las pantallas de la central en un solo JSON. hora = minutos de reloj real. */
public record CentralView(double time, ConfigView config, String mode, String engineFailure, int pendingAccounts,
                           int openIncidents, List<CourierView> couriers, List<DeliveryView> deliveries, List<Alert> alerts) {
    /** Compatibilidad con la versión anterior de las pantallas. */
    public boolean engineActive() { return !"MANUAL".equals(mode); }
}
