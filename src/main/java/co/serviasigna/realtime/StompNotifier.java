package co.serviasigna.realtime;

import co.serviasigna.service.DispatchService;
import co.serviasigna.service.Notifier;
import co.serviasigna.users.UserAccountService;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Manda los cambios a las pantallas apenas ocurren, sin que tengan que preguntar cada segundo.
 * Para no saturar, el estado completo de la central se manda como máximo cada 0,4 s (y cada 5 s aunque no cambie nada,
 * para que el reloj y los contadores sigan al día). Los avisos puntuales salen de inmediato.
 */
@Component
public class StompNotifier implements Notifier {
    private static final Logger log = LoggerFactory.getLogger(StompNotifier.class);
    private final SimpMessagingTemplate stomp;
    private final ObjectProvider<DispatchService> dispatch;     // perezoso: DispatchService también depende de este notificador
    private final ObjectProvider<UserAccountService> users;
    private final AtomicBoolean pending = new AtomicBoolean(true);
    private volatile long lastSend;

    public StompNotifier(SimpMessagingTemplate stomp, ObjectProvider<DispatchService> dispatch, ObjectProvider<UserAccountService> users) {
        this.stomp = stomp; this.dispatch = dispatch; this.users = users;
    }

    @Override
    public void changed() { pending.set(true); }

    @Override
    public void toCourier(String username, String type, Map<String, Object> data) {
        send("/queue/notices", username, notice(type, data));
    }

    @Override
    public void toCentral(String type, Map<String, Object> data) {
        try { stomp.convertAndSend("/topic/central/notices", notice(type, data)); }
        catch (RuntimeException e) { log.debug("No se pudo avisar a la central: {}", e.getMessage()); }
    }

    @Scheduled(fixedDelay = 400, initialDelay = 3000)
    public void push() {
        long now = System.currentTimeMillis();
        if (!pending.getAndSet(false) && now - lastSend < 5000) return;
        lastSend = now;
        try {
            DispatchService d = dispatch.getIfAvailable();
            UserAccountService u = users.getIfAvailable();
            if (d == null || u == null) return;
            stomp.convertAndSend("/topic/central", d.view(u.countPending()));
        } catch (RuntimeException e) {
            log.warn("No se pudo enviar el estado a la central: {}", e.getMessage());
        }
    }

    private void send(String destination, String username, Object load) {
        try { stomp.convertAndSendToUser(username, destination, load); }
        catch (RuntimeException e) { log.debug("No se pudo avisar a {}: {}", username, e.getMessage()); }
    }

    private static Map<String, Object> notice(String type, Map<String, Object> data) {
        Map<String, Object> m = new HashMap<>(data == null ? Map.of() : data);
        m.put("type", type);
        return m;
    }
}
