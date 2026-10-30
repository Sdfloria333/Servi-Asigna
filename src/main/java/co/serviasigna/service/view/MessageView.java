package co.serviasigna.service.view;

/** Un mensaje de la conversación central ↔ domiciliario (RF-M4-06). */
public record MessageView(long id, long courierId, boolean fromCentral, String author, String text, double date, boolean read) {}
