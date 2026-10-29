package co.serviasigna.engine;

import co.serviasigna.domain.GeoPoint;
import co.serviasigna.service.Settings;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Matriz de tiempos y distancias por calles reales (RF-M5-03) usando OSRM (OpenStreetMap).
 * Una pasada del motor pide UNA matriz con todos los puntos que le interesan (posiciones y paradas).
 * Si OSRM no responde en 2 s, o está apagado en los parámetros, se usa la estimación (línea recta × factor):
 * el motor nunca se detiene por culpa del servicio de mapas.
 */
@Component
public class Routing {
    private static final Logger log = LoggerFactory.getLogger(Routing.class);
    private static final int MAX_POINTS = 90;                // límite del servidor público de OSRM
    private final Settings settings;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
    private final ObjectMapper json = new ObjectMapper();
    private volatile long pausedUntil = 0;                  // si OSRM falló, se descansa 5 minutos antes de reintentar
    private volatile String lastProblem;

    public Routing(Settings settings) { this.settings = settings; }

    /** Distancias estimadas (sin internet). */
    public Distances estimated() { return Distances.estimated(settings.kmPerMin(), settings.detourFactor()); }

    /** Matriz para estos puntos (o la estimación si OSRM no está disponible). */
    public Distances target(List<GeoPoint> points) {
        Distances fallback = estimated();
        List<GeoPoint> unique = new ArrayList<>(new LinkedHashSet<>(points));
        if (!settings.flag("ruteo.usar-osrm") || unique.size() < 2 || unique.size() > MAX_POINTS || System.currentTimeMillis() < pausedUntil)
            return fallback;
        try {
            StringBuilder coords = new StringBuilder();
            for (GeoPoint p : unique) {
                if (!coords.isEmpty()) coords.append(';');
                coords.append(String.format(Locale.ROOT, "%.6f,%.6f", p.lng(), p.lat()));
            }
            String url = settings.text("ruteo.osrm-url").replaceAll("/+$", "") + "/table/v1/driving/" + coords + "?annotations=duration,distance";
            HttpRequest req = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(2)).GET().build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200) throw new IllegalStateException("OSRM respondió " + res.statusCode());
            JsonNode root = json.readTree(res.body());
            JsonNode dur = root.path("durations"), dist = root.path("distances");
            int n = unique.size();
            double[][] km = new double[n][n], min = new double[n][n];
            for (int i = 0; i < n; i++) for (int j = 0; j < n; j++) {
                JsonNode d = dist.path(i).path(j), t = dur.path(i).path(j);
                km[i][j] = d.isNumber() ? d.asDouble() / 1000.0 : -1;     // -1 = sin dato: se usa la estimación
                min[i][j] = t.isNumber() ? t.asDouble() / 60.0 * 1.15 : -1; // OSRM es para carro: +15% por tráfico de ciudad
            }
            lastProblem = null;
            return new DistanceMatrix(unique, km, min, fallback);
        } catch (Exception e) {
            pausedUntil = System.currentTimeMillis() + 5 * 60_000;
            if (lastProblem == null) log.warn("OSRM no disponible ({}); el motor usa la estimación por 5 minutos", e.getMessage());
            lastProblem = e.getMessage();
            return fallback;
        }
    }

    /** Para mostrar en la pantalla de configuración. */
    public String status() {
        if (!settings.flag("ruteo.usar-osrm")) return "Apagado: el motor usa la estimación en línea recta";
        if (System.currentTimeMillis() < pausedUntil) return "OSRM no respondió (" + lastProblem + "); usando la estimación";
        return "OSRM activo";
    }
}
