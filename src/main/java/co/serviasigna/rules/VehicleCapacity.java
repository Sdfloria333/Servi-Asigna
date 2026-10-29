package co.serviasigna.rules;

import co.serviasigna.engine.RouteContext;
import org.springframework.stereotype.Component;

/** Prohibición: la carga a bordo (en unidades) nunca supera la capacidad del vehículo (RN-04). */
@Component
public class VehicleCapacity implements HardRule {
    public String name() { return "Capacidad del vehículo"; }
    public boolean satisfies(RouteContext c) { return c.maxLoad() <= c.courier().getCapacity(); }
}
