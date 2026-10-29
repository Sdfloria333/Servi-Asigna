package co.serviasigna.service;

import co.serviasigna.domain.AssignmentMode;
import co.serviasigna.domain.DbCoded;
import co.serviasigna.domain.Setting;
import co.serviasigna.persistence.SettingRepository;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Parámetros operativos guardados en la tabla PARAMETRO y editables por el administrador (RF-M2-08).
 * Si falta alguno en la base de datos, se crea con el valor por defecto de la lista DEFINICIONES.
 */
@Component
public class Settings {
    /** clave, valor por defecto, tipo (N = número, B = sí/no, T = texto, M = modo), descripción */
    public record Definition(String key, String defaultValue, char type, String description) {}

    public static final List<Definition> DEFINITIONS = List.of(
        new Definition("motor.modo", "AUTOMATICO", 'M', "Modo de asignación: MANUAL, DIFUSION o AUTOMATICO (RF-M5-01)"),
        new Definition("oferta.segundos", "30", 'N', "Segundos que tiene el domiciliario para aceptar una oferta (RN-05)"),
        new Definition("oferta.rechazos-alerta", "3", 'N', "Rechazos o vencimientos seguidos que generan alerta (RN-05)"),
        new Definition("oferta.difusion-candidatos", "3", 'N', "A cuántos domiciliarios se ofrece a la vez en modo Difusión"),
        new Definition("motor.desvio-max-km", "2", 'N', "Kilómetros extra máximos para ofrecer a quien ya va en ruta (RN-06)"),
        new Definition("motor.reordenar-cada-seg", "20", 'N', "Cada cuántos segundos se reordenan las paradas (RF-M5-07)"),
        new Definition("motor.reserva-libres", "true", 'B', "Preferir juntar servicios y dejar domiciliarios libres en reserva"),
        new Definition("operacion.velocidad-kmh", "25", 'N', "Velocidad promedio de una moto en la ciudad (km/h)"),
        new Definition("operacion.factor-rodeo", "1.3", 'N', "Cuánto más largo es ir por calles que en línea recta (si no hay OSRM)"),
        new Definition("operacion.min-recogida", "3", 'N', "Minutos que se demora recogiendo un pedido"),
        new Definition("operacion.min-entrega", "2", 'N', "Minutos que se demora entregando un pedido"),
        new Definition("operacion.min-max-recogida", "20", 'N', "Tiempo máximo para recoger desde que el pedido está listo (RN-12)"),
        new Definition("servicio.limite-defecto-min", "40", 'N', "Minutos para entregar que se proponen al crear un servicio"),
        new Definition("gps.intervalo-segundos", "10", 'N', "Cada cuántos segundos manda su posición el celular (RF-M6-10)"),
        new Definition("gps.precision-maxima-metros", "150", 'N', "Puntos GPS con más margen de error se ignoran"),
        new Definition("gps.retencion-dias", "90", 'N', "Días que se guarda el historial de GPS antes de borrarlo (RNF-10)"),
        new Definition("llegada-metros", "80", 'N', "A cuántos metros de la parada se considera que llegó"),
        new Definition("evidencia.exigir-pin", "true", 'B', "Exigir el PIN del cliente al entregar (RF-M6-06)"),
        new Definition("evidencia.exigir-foto", "false", 'B', "Exigir foto al entregar"),
        new Definition("evidencia.exigir-firma", "false", 'B', "Exigir firma del cliente al entregar"),
        new Definition("alerta.minutos-sin-asignar", "5", 'N', "Minutos en cola que generan alerta de servicio sin asignar (RF-M4-04)"),
        new Definition("alerta.segundos-sin-senal", "30", 'N', "Segundos sin reportar que marcan a un domiciliario como sin señal"),
        new Definition("ruteo.usar-osrm", "true", 'B', "Usar OSRM (calles reales) para distancias y tiempos del motor (RF-M5-03)"),
        new Definition("ruteo.osrm-url", "https://router.project-osrm.org", 'T', "Servidor OSRM (puede ser uno propio)"),
        new Definition("mapa.teselas-url", "https://tile.openstreetmap.org/{z}/{x}/{y}.png", 'T', "Teselas del mapa (OpenStreetMap)"),
        new Definition("mapa.nominatim-url", "https://nominatim.openstreetmap.org", 'T', "Buscador de direcciones (Nominatim)"),
        new Definition("linea-base.tiempo-asignacion-min", "8", 'N', "Línea base: minutos para asignar un servicio por radio (antes del sistema)"),
        new Definition("linea-base.tiempo-entrega-min", "50", 'N', "Línea base: minutos de creación a entrega"),
        new Definition("linea-base.km-por-servicio", "5", 'N', "Línea base: kilómetros recorridos por servicio"),
        new Definition("linea-base.cumplimiento-sla", "70", 'N', "Línea base: % de servicios a tiempo"),
        new Definition("linea-base.intervenciones-hora", "15", 'N', "Línea base: intervenciones manuales de la central por hora")
    );

    private final SettingRepository repo;
    private final Map<String, String> values = new ConcurrentHashMap<>();
    private final Map<String, Definition> byKey = new LinkedHashMap<>();

    public Settings(SettingRepository repo) {
        this.repo = repo;
        for (Definition d : DEFINITIONS) { byKey.put(d.key(), d); values.put(d.key(), d.defaultValue()); }
    }

    /** Lee la tabla y crea lo que falte (se llama al arrancar). */
    public void load() {
        Set<String> inDatabase = new HashSet<>();
        for (Setting p : repo.findAll()) {
            inDatabase.add(p.getKey());
            if (byKey.containsKey(p.getKey())) values.put(p.getKey(), p.getValue());
        }
        for (Definition d : DEFINITIONS)
            if (!inDatabase.contains(d.key())) repo.save(new Setting(d.key(), d.defaultValue(), d.description()));
    }

    /** Cambia un valor (lo valida según su tipo y lo guarda). */
    public void update(String key, String value) {
        Definition d = byKey.get(key);
        if (d == null) throw new IllegalArgumentException("No existe el parámetro " + key);
        String v = value == null ? "" : value.trim();
        switch (d.type()) {
            case 'N' -> {
                double n;
                try { n = Double.parseDouble(v.replace(',', '.')); } catch (NumberFormatException e) { throw new IllegalArgumentException(key + " debe ser un número"); }
                if (n < 0) throw new IllegalArgumentException(key + " no puede ser negativo");
                v = Double.toString(n).replaceAll("\\.0$", "");
            }
            case 'B' -> {
                if (!v.equalsIgnoreCase("true") && !v.equalsIgnoreCase("false")) throw new IllegalArgumentException(key + " debe ser true o false");
                v = v.toLowerCase();
            }
            case 'M' -> {
                try { v = DbCoded.fromDbCode(AssignmentMode.class, v).dbCode(); } catch (IllegalArgumentException e) { throw new IllegalArgumentException("Modo inválido: usa MANUAL, DIFUSION o AUTOMATICO"); }
            }
            default -> { if (v.isEmpty() || v.length() > 200) throw new IllegalArgumentException(key + " no puede quedar vacío"); }
        }
        Setting p = repo.findById(key).orElse(new Setting(key, v, d.description()));
        p.setValue(v);
        repo.save(p);
        values.put(key, v);
    }

    public List<Map<String, String>> list() {
        List<Map<String, String>> list = new ArrayList<>();
        for (Definition d : DEFINITIONS)
            list.add(Map.of("key", d.key(), "value", values.get(d.key()), "type", String.valueOf(d.type()), "description", d.description()));
        return list;
    }

    public String text(String key) { return values.get(key); }
    public double number(String key) { return Double.parseDouble(values.get(key)); }
    public int integer(String key) { return (int) Math.round(number(key)); }
    public boolean flag(String key) { return Boolean.parseBoolean(values.get(key)); }

    // ---- Atajos con nombre ----
    public AssignmentMode mode() { return DbCoded.fromDbCode(AssignmentMode.class, text("motor.modo")); }
    public double kmPerMin() { return number("operacion.velocidad-kmh") / 60.0; }
    public double detourFactor() { return Math.max(1, number("operacion.factor-rodeo")); }
    public double pickupMin() { return number("operacion.min-recogida"); }
    public double dropoffMin() { return number("operacion.min-entrega"); }
    public double maxPickupMin() { return number("operacion.min-max-recogida"); }
    public double arrivalMeters() { return number("llegada-metros"); }
    public int gpsIntervalSec() { return Math.max(2, integer("gps.intervalo-segundos")); }
    public double maxAccuracyM() { return number("gps.precision-maxima-metros"); }
    public boolean requirePin() { return flag("evidencia.exigir-pin"); }
    public boolean requirePhoto() { return flag("evidencia.exigir-foto"); }
    public boolean requireSignature() { return flag("evidencia.exigir-firma"); }
    public double offerSeconds() { return Math.max(5, number("oferta.segundos")); }
    public int rejectionsAlert() { return Math.max(1, integer("oferta.rechazos-alerta")); }
    public int broadcastCandidates() { return Math.max(1, integer("oferta.difusion-candidatos")); }
    public double maxDetourKm() { return number("motor.desvio-max-km"); }
    public double minutesWithoutSignal() { return number("alerta.segundos-sin-senal") / 60.0; }
}
