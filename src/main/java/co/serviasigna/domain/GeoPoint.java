package co.serviasigna.domain;

/**
 * Un punto de la Tierra en grados decimales (WGS84), el mismo formato que usan el GPS del celular y Google Maps.
 * <p>
 * EN: A point on Earth in decimal degrees (WGS84), the same format used by the phone GPS and Google Maps.
 *
 * @param lat latitud, norte/sur (Valledupar ≈ 10.46) / latitude, north/south (Valledupar ≈ 10.46)
 * @param lng longitud, oriente/occidente (Valledupar ≈ -73.25) / longitude, east/west (Valledupar ≈ -73.25)
 */
public record GeoPoint(double lat, double lng) {
    /** Radio medio de la Tierra en km. EN: Mean Earth radius in km. */
    private static final double EARTH_RADIUS_KM = 6371.0088;

    /**
     * Calcula la distancia en línea recta con la fórmula de Haversine.
     * <p>
     * EN: Computes the straight-line distance with the Haversine formula.
     *
     * @param other el otro punto / the other point
     * @return la distancia en kilómetros / the distance in kilometers
     */
    public double distanceKm(GeoPoint other) {
        double f1 = Math.toRadians(lat), f2 = Math.toRadians(other.lat);
        double df = f2 - f1, dl = Math.toRadians(other.lng - lng);
        double a = Math.sin(df / 2) * Math.sin(df / 2) + Math.cos(f1) * Math.cos(f2) * Math.sin(dl / 2) * Math.sin(dl / 2);
        return 2 * EARTH_RADIUS_KM * Math.asin(Math.min(1, Math.sqrt(a)));
    }

    /**
     * Calcula la distancia en línea recta, en metros.
     * <p>
     * EN: Computes the straight-line distance, in meters.
     *
     * @param other el otro punto / the other point
     * @return la distancia en metros / the distance in meters
     */
    public double distanceM(GeoPoint other) { return distanceKm(other) * 1000; }

    /**
     * Indica si las coordenadas son números reales dentro de los rangos válidos.
     * <p>
     * EN: Tells whether the coordinates are real numbers within the valid ranges.
     *
     * @return true si el punto es válido / true if the point is valid
     */
    public boolean isValid() {
        return Double.isFinite(lat) && Double.isFinite(lng) && Math.abs(lat) <= 90 && Math.abs(lng) <= 180;
    }
}
