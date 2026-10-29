package co.serviasigna.service.view;

/**
 * Una alerta para la central (RF-M4-04, RF-M9-02). nivel: 3 urgente, 2 atención, 1 aviso.
 * id es estable mientras dure la situación (así la pantalla sabe cuáles son nuevas).
 */
public record Alert(String id, int level, String type, String text, String link) {}
