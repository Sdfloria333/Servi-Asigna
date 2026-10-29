package co.serviasigna.rules;

import co.serviasigna.engine.RouteContext;

/** Una PROHIBICIÓN: si cumple() da false, esa ruta no se puede usar. */
public interface HardRule {
    String name();
    boolean satisfies(RouteContext c);
}
