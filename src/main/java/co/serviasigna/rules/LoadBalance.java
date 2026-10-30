package co.serviasigna.rules;

import co.serviasigna.engine.RouteContext;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** EJEMPLO opcional: castiga que un solo domiciliario cargue muchos servicios. */
@Component
@ConditionalOnProperty(name = "serviasigna.rules.load-balance.enabled", havingValue = "true")
public class LoadBalance implements SoftRule {
    public String name() { return "Balance de carga"; }
    public double cost(RouteContext c) { return 2.0 * c.deliveries().size(); }
}
