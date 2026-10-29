package co.serviasigna.engine;

import co.serviasigna.domain.*;
import java.util.*;

/**
 * El algoritmo de asignación (documentado en docs/D-06-motor.md):
 *   - candidatos(): para un servicio en cola, prueba a cada domiciliario elegible en todas las posiciones de su ruta
 *     (recoger en a, entregar en b ≥ a) y ordena por el costo extra mínimo (RF-M5-03). A quien ya va en ruta solo
 *     se le propone si no atrasa lo que ya aceptó y el desvío está dentro del límite (RF-M5-05, RN-06).
 *   - reordenar(): mejora el orden de las paradas de un domiciliario sacando y reinsertando cada servicio (RF-M5-07).
 * Lo que un domiciliario ya aceptó nunca se pasa a otro: solo la central lo revoca (RN-15).
 */
public class AssignmentEngine {
    private final RouteEvaluator evaluator;

    public AssignmentEngine(RouteEvaluator evaluator) { this.evaluator = evaluator; }

    public RouteEvaluator evaluator() { return evaluator; }

    /** Un domiciliario posible para un servicio: la ruta que le quedaría y cuánto cuesta meterlo. */
    public record Candidate(Courier courier, List<Stop> route, RouteEvaluation eval, double extraCost, double kmExtra) {}

    /**
     * Candidatos ordenados de menor a mayor costo. maxDetourKm limita cuánto se alarga la ruta de quien ya va en ruta.
     */
    public List<Candidate> candidates(Delivery s, List<Courier> couriers, Environment e, double maxDetourKm) {
        List<Candidate> list = new ArrayList<>();
        for (Courier d : couriers) {
            Candidate c = bestInsertion(d, s, e, !d.getRoute().isEmpty(), maxDetourKm);
            if (c != null) list.add(c);
        }
        list.sort(Comparator.comparingDouble(Candidate::extraCost));
        return list;
    }

    /** La mejor forma de meter el servicio en la ruta de este domiciliario (null si ninguna cumple las reglas). */
    public Candidate bestInsertion(Courier d, Delivery s, Environment e, boolean requireCompatible, double maxDetourKm) {
        List<Stop> route = d.getRoute();
        RouteEvaluation base = evaluator.evaluate(d, route, e);
        int n = route.size();
        int from = n > 0 ? 1 : 0;                          // la parada a la que ya va no se le cambia
        Candidate best = null;
        for (int a = from; a <= n; a++) {
            for (int b = a; b <= n; b++) {
                List<Stop> candidateRoute = new ArrayList<>(route);
                candidateRoute.add(b, new Stop(s, StopType.DROPOFF));
                candidateRoute.add(a, new Stop(s, StopType.PICKUP));
                RouteEvaluation ev = evaluator.evaluate(d, candidateRoute, e);
                if (!ev.ok()) continue;
                double kmExtra = ev.km() - base.km();
                if (requireCompatible) {
                    if (kmExtra > maxDetourKm) continue;                                   // RN-06: desvío máximo
                    if (delaysAccepted(route, base, ev)) continue;                         // RN-06: no comprometer lo aceptado
                }
                double extra = ev.cost() - (base.ok() ? base.cost() : 0);
                if (best == null || extra < best.extraCost()) best = new Candidate(d, candidateRoute, ev, extra, kmExtra);
            }
        }
        return best;
    }

    /** ¿Algún servicio que ya estaba en la ruta quedaría más de medio minuto más tarde de su límite? */
    private boolean delaysAccepted(List<Stop> route, RouteEvaluation before, RouteEvaluation after) {
        Set<Delivery> seen = new HashSet<>();
        for (Stop p : route) {
            if (!seen.add(p.delivery())) continue;
            if (after.delayOf(p.delivery()) > before.delayOf(p.delivery()) + 0.5) return true;
        }
        return false;
    }

    /**
     * Mejora el orden de la ruta: saca cada servicio y lo vuelve a meter donde menos cuesta, hasta que no mejore.
     * La primera parada (a la que ya va) queda fija.
     */
    public List<Stop> reorder(Courier d, Environment e) {
        List<Stop> route = new ArrayList<>(d.getRoute());
        if (route.size() < 3) return route;
        RouteEvaluation current = evaluator.evaluate(d, route, e);
        if (!current.ok()) return route;
        for (int returnTrip = 0; returnTrip < 3; returnTrip++) {
            boolean improved = false;
            List<Delivery> deliveries = route.stream().skip(1).map(Stop::delivery).distinct().toList();
            for (Delivery s : deliveries) {
                Stop fixed = route.get(0);
                List<Stop> sin = new ArrayList<>();
                for (int i = 1; i < route.size(); i++) if (route.get(i).delivery() != s) sin.add(route.get(i));
                boolean hasPickup = route.stream().skip(1).anyMatch(p -> p.delivery() == s && p.type() == StopType.PICKUP);
                boolean firstIsTheirs = fixed.delivery() == s;          // ya va a recogerlo: solo se mueve la entrega
                List<Stop> bestRoute = null;
                double bestCost = current.cost() - 1e-6;
                int n = sin.size();
                for (int a = 0; a <= n; a++) {
                    for (int b = a; b <= n; b++) {
                        if (!hasPickup && a > 0) break;             // solo entrega: basta probar b
                        List<Stop> proof = new ArrayList<>(sin);
                        proof.add(b, new Stop(s, StopType.DROPOFF));
                        if (hasPickup && !firstIsTheirs) proof.add(a, new Stop(s, StopType.PICKUP));
                        proof.add(0, fixed);
                        RouteEvaluation ev = evaluator.evaluate(d, proof, e);
                        if (ev.ok() && ev.cost() < bestCost) { bestCost = ev.cost(); bestRoute = proof; }
                    }
                }
                if (bestRoute != null) { route = bestRoute; current = evaluator.evaluate(d, route, e); improved = true; }
            }
            if (!improved) break;
        }
        return route;
    }
}
