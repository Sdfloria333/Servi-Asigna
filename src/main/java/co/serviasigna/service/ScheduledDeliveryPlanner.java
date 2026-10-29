package co.serviasigna.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Cada segundo: vence ofertas, mete los programados a la cola, sube la prioridad de lo atrasado,
 * corre el motor y reordena rutas (ver DispatchService.tick).
 */
@Component
public class ScheduledDeliveryPlanner {
    private static final Logger log = LoggerFactory.getLogger(ScheduledDeliveryPlanner.class);
    private final DispatchService dispatch;
    private volatile boolean ready;

    public ScheduledDeliveryPlanner(DispatchService dispatch) { this.dispatch = dispatch; }

    /** DemoDataLoader lo enciende cuando terminó de cargar la operación. */
    public void start() { ready = true; }

    @Scheduled(fixedRate = 1000)
    public void tick() {
        if (!ready) return;
        try { dispatch.tick(); }
        catch (RuntimeException e) { log.error("Error en el planificador", e); }
    }
}
