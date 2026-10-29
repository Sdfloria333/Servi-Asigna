package co.serviasigna.engine;

import co.serviasigna.domain.Stop;

/** Cuándo llega y cuándo sale el domiciliario de una parada (minutos). */
public record Schedule(Stop stop, double arrives, double sale) {}
