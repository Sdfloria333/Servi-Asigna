package co.serviasigna.service.view;

import java.util.List;

/** Todo lo que necesita la app de UN domiciliario. */
public record MyShiftView(double time, ConfigView config, CourierView courier, List<DeliveryView> deliveries,
                           CourierOfferView offer, List<MessageView> messages, List<String> templates, List<String> notices) {}
