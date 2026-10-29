package co.serviasigna.rules;

import co.serviasigna.engine.RouteContext;
import org.springframework.stereotype.Component;

/** Cada kilómetro recorrido cuesta puntos. A ~25 km/h, 1 km ≈ 2,4 min, por eso el peso es 2,5 (1 punto ≈ 1 minuto). */
@Component
public class TravelledDistance implements SoftRule {
    private static final double POINTS_PER_KM = 2.5;
    public String name() { return "Distancia recorrida"; }
    public double cost(RouteContext c) { return c.km() * POINTS_PER_KM; }
}
