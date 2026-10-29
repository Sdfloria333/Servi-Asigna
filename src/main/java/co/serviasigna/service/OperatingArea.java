package co.serviasigna.service;

import co.serviasigna.domain.GeoPoint;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Dónde opera la central: un centro (para abrir el mapa y para ubicar a quien aún no manda GPS)
 * y un rectángulo (sur-oeste / nor-este) fuera del cual no se aceptan servicios. Valores en application.properties.
 */
@Component
public class OperatingArea {
    private final GeoPoint center, southWest, northEast;

    public OperatingArea(@Value("${serviasigna.center.lat}") double centerLat, @Value("${serviasigna.center.lng}") double centerLng,
                         @Value("${serviasigna.area.lat-min}") double latMin, @Value("${serviasigna.area.lat-max}") double latMax,
                         @Value("${serviasigna.area.lng-min}") double lngMin, @Value("${serviasigna.area.lng-max}") double lngMax) {
        this.center = new GeoPoint(centerLat, centerLng);
        this.southWest = new GeoPoint(latMin, lngMin);
        this.northEast = new GeoPoint(latMax, lngMax);
        if (latMin >= latMax || lngMin >= lngMax) throw new IllegalArgumentException("serviasigna.area.*: los mínimos deben ser menores que los máximos");
        if (!contains(center)) throw new IllegalArgumentException("serviasigna.center.* debe quedar dentro de serviasigna.area.*");
    }

    public boolean contains(GeoPoint p) {
        return p != null && p.isValid()
                && p.lat() >= southWest.lat() && p.lat() <= northEast.lat()
                && p.lng() >= southWest.lng() && p.lng() <= northEast.lng();
    }

    public GeoPoint getCenter() { return center; }
    public GeoPoint getSouthWest() { return southWest; }
    public GeoPoint getNorthEast() { return northEast; }
}
