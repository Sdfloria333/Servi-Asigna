package co.serviasigna.service.view;

import co.serviasigna.domain.GeoPoint;

/** Lo que el navegador necesita saber de la configuración: mapa (OpenStreetMap), GPS y pruebas de entrega. */
public record ConfigView(GeoPoint center, GeoPoint southWest, GeoPoint northEast, double kmPerMin, double detourFactor,
                          int gpsIntervalSec, int arrivalMeters,
                          boolean requirePin, boolean requirePhoto, boolean requireSignature, double offerSeconds,
                          String tilesUrl, String nominatimUrl, String osrmUrl) {}
