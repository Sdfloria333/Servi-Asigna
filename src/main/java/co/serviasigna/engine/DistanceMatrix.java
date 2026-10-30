package co.serviasigna.engine;

import co.serviasigna.domain.GeoPoint;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Matriz precalculada para un grupo de puntos (una pasada del motor). Lo que no esté en la matriz
 * se calcula con la estimación de respaldo, así el motor nunca se queda sin respuesta.
 */
public class DistanceMatrix implements Distances {
    private final Map<GeoPoint, Integer> index = new HashMap<>();
    private final double[][] km, minutes;
    private final Distances fallback;

    public DistanceMatrix(List<GeoPoint> points, double[][] km, double[][] minutes, Distances fallback) {
        for (int i = 0; i < points.size(); i++) index.putIfAbsent(points.get(i), i);
        this.km = km; this.minutes = minutes; this.fallback = fallback;
    }

    private Integer i(GeoPoint p) { return index.get(p); }

    public double km(GeoPoint a, GeoPoint b) {
        Integer x = i(a), y = i(b);
        if (x == null || y == null || km[x][y] < 0) return fallback.km(a, b);
        return km[x][y];
    }

    public double minutes(GeoPoint a, GeoPoint b) {
        Integer x = i(a), y = i(b);
        if (x == null || y == null || minutes[x][y] < 0) return fallback.minutes(a, b);
        return minutes[x][y];
    }
}
