package co.serviasigna.engine;

import co.serviasigna.domain.GeoPoint;

/**
 * De dónde saca el motor la distancia y el tiempo entre dos puntos (RF-M5-03: matriz de tiempos y distancias).
 * Hay dos: la estimada (línea recta × factor de rodeo, siempre funciona) y la de OSRM (calles reales, si hay internet).
 */
public interface Distances {
    double km(GeoPoint a, GeoPoint b);
    double minutes(GeoPoint a, GeoPoint b);

    /** Estimación: línea recta (Haversine) × factor de rodeo, a velocidad promedio. */
    static Distances estimated(double kmPerMin, double detourFactor) {
        return new Distances() {
            public double km(GeoPoint a, GeoPoint b) { return a.distanceKm(b) * detourFactor; }
            public double minutes(GeoPoint a, GeoPoint b) { return km(a, b) / kmPerMin; }
        };
    }
}
