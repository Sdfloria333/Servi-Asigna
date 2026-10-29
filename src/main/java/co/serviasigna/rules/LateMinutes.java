package co.serviasigna.rules;

import co.serviasigna.engine.RouteContext;
import org.springframework.stereotype.Component;

@Component
public class LateMinutes implements SoftRule {
    private static final double WEIGHT = 1;   // sube este número para castigar más el atraso
    public String name() { return "Minutos de atraso"; }
    public double cost(RouteContext c) { return c.delay() * WEIGHT; }
}
