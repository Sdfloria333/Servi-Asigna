package co.serviasigna.engine;

import co.serviasigna.domain.Courier;
import co.serviasigna.domain.Stop;
import co.serviasigna.domain.Delivery;
import java.util.List;

/** Todo lo que una regla puede mirar de una ruta candidata. */
public record RouteContext(
        Courier courier,          // el domiciliario
        List<Stop> route,          // sus paradas en orden
        List<Delivery> deliveries,   // servicios distintos que lleva
        List<Schedule> schedules,     // cuándo llega/sale de cada parada
        double km,                  // kilómetros totales que recorrería
        double delay,              // minutos de atraso sumados, ponderados por prioridad
        int maxLoad,            // máximo de unidades de carga a bordo al mismo tiempo (RN-04)
        double endsAt,            // hora en que termina toda la ruta
        boolean reserveActive) {}
