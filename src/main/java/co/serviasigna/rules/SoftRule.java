package co.serviasigna.rules;

import co.serviasigna.engine.RouteContext;

/** Un COSTO en puntos (1 punto ≈ 1 minuto de viaje o de atraso). El motor busca el menor total. */
public interface SoftRule {
    String name();
    double cost(RouteContext c);
}
