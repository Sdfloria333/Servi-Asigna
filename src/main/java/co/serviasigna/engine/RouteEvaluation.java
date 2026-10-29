package co.serviasigna.engine;

import co.serviasigna.domain.Delivery;
import co.serviasigna.domain.StopType;
import java.util.List;

/** Resultado de simular una ruta. Si ok es false, "razon" dice qué regla dura la prohíbe. */
public record RouteEvaluation(boolean ok, String reason, double cost, double km, double delay, List<Schedule> schedules) {
    static RouteEvaluation rejected(String reason, List<Schedule> schedules) {
        return new RouteEvaluation(false, reason, 0, 0, 0, schedules);
    }

    /** Minutos que se pasaría del límite la entrega de este servicio en esta ruta (0 si llega a tiempo o no está). */
    public double delayOf(Delivery s) {
        for (Schedule h : schedules)
            if (h.stop().delivery() == s && h.stop().type() == StopType.DROPOFF) return Math.max(0, h.sale() - s.getDeadline());
        return 0;
    }
}
