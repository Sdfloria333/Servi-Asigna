package co.serviasigna.engine;

import co.serviasigna.domain.*;
import co.serviasigna.rules.SoftRule;
import co.serviasigna.rules.HardRule;
import java.util.*;

/**
 * Simula una ruta desde donde está AHORA el domiciliario y calcula horarios, km, atrasos y carga.
 * Luego le pregunta a las reglas si la ruta es válida (duras) y cuánto cuesta (blandas).
 * No sabe nada de Spring ni de HTTP: solo recibe reglas por el constructor.
 */
public class RouteEvaluator {
    private final List<HardRule> hardRules;
    private final List<SoftRule> softRules;

    public RouteEvaluator(List<HardRule> hardRules, List<SoftRule> softRules) {
        this.hardRules = hardRules;
        this.softRules = softRules;
    }

    public List<String> hardRuleNames() { return hardRules.stream().map(HardRule::name).toList(); }
    public List<String> softRuleNames() { return softRules.stream().map(SoftRule::name).toList(); }

    /** Peso del atraso según la prioridad (RF-M5-04: cola con prioridad por SLA). */
    static double weight(Priority p) {
        return switch (p) { case NORMAL -> 1; case HIGH -> 2; case URGENT -> 4; };
    }

    public RouteEvaluation evaluate(Courier courier, List<Stop> route, Environment e) {
        double clock = e.time();
        GeoPoint pos = courier.getPosition();
        double km = 0, delay = 0;

        // Lo que ya lleva a bordo: su entrega está en la ruta pero su recogida ya no
        Set<Delivery> toPickUp = new HashSet<>();
        for (Stop p : route) if (p.type() == StopType.PICKUP) toPickUp.add(p.delivery());
        int load = 0;
        for (Stop p : route) if (p.type() == StopType.DROPOFF && !toPickUp.contains(p.delivery())) load += p.delivery().getUnits();
        int maxLoad = load;

        List<Schedule> schedules = new ArrayList<>();
        for (Stop p : route) {
            Delivery s = p.delivery();
            GeoPoint destination = p.destination();
            km += e.distances().km(pos, destination);
            clock += e.distances().minutes(pos, destination);                   // viaje
            double arrives = clock;

            if (p.type() == StopType.PICKUP) {
                clock = Math.max(clock, s.getReadyAt()) + e.pickupMin();     // espera si no está listo + recoge
                load += s.getUnits();
                maxLoad = Math.max(maxLoad, load);
                delay += Math.max(0, arrives - s.getPickupDeadline()) * 0.5 * weight(s.getPriority());   // recogida tardía (RN-12)
            } else {
                clock += e.dropoffMin();
                load -= s.getUnits();
                if (!s.isReturnTrip()) delay += Math.max(0, clock - s.getDeadline()) * weight(s.getPriority());
            }
            schedules.add(new Schedule(p, arrives, clock));
            pos = destination;
        }

        List<Delivery> deliveries = route.stream().map(Stop::delivery).distinct().toList();
        RouteContext c = new RouteContext(courier, route, deliveries, schedules, km, delay, maxLoad, clock, e.reserveActive());

        for (HardRule r : hardRules) {
            if (!r.satisfies(c)) return RouteEvaluation.rejected(r.name(), schedules);
        }
        double cost = 0;
        for (SoftRule r : softRules) cost += r.cost(c);
        return new RouteEvaluation(true, null, cost, km, delay, schedules);
    }
}
