package co.serviasigna.rules;

import co.serviasigna.engine.RouteContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** EJEMPLO opcional: máximo N servicios por domiciliario. Se activa en application.properties. */
@Component
@ConditionalOnProperty(name = "serviasigna.rules.max-deliveries.enabled", havingValue = "true")
public class MaxDeliveries implements HardRule {
    private final int maximum;
    public MaxDeliveries(@Value("${serviasigna.rules.max-deliveries.maximum:4}") int maximum) { this.maximum = maximum; }
    public String name() { return "Máximo " + maximum + " servicios"; }
    public boolean satisfies(RouteContext c) { return c.deliveries().size() <= maximum; }
}
