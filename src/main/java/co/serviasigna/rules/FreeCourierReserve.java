package co.serviasigna.rules;

import co.serviasigna.engine.RouteContext;
import org.springframework.stereotype.Component;

/** Usar un domiciliario libre "cuesta" 5 puntos: el motor prefiere juntar servicios en los que ya van en ruta. */
@Component
public class FreeCourierReserve implements SoftRule {
    public String name() { return "Reserva de libres"; }
    public double cost(RouteContext c) {
        return (c.reserveActive() && c.courier().isFree() && !c.route().isEmpty()) ? 5 : 0;
    }
}
