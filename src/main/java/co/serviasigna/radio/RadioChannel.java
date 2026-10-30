package co.serviasigna.radio;

import co.serviasigna.users.UserAccount;
import co.serviasigna.users.UserAccountService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;

/**
 * Radio tipo walkie-talkie (como Zello): un solo canal para la central y todos los domiciliarios en línea.
 *
 * Cómo funciona:
 *   1. Cada pantalla abierta se conecta a /ws/radio (queda "en el canal").
 *   2. Para hablar, el navegador pide el turno: {"t":"hablar"}. Si nadie más está hablando, se le concede.
 *   3. Mientras mantiene el botón, manda pedacitos de voz (binario: PCM 16 bits, 16 kHz, mono)
 *      y el servidor los reenvía al instante a todos los demás.
 *   4. Al soltar: {"t":"soltar"} y el canal queda libre.
 * El servidor no guarda audio: solo lo reenvía.
 */
@Component
public class RadioChannel extends AbstractWebSocketHandler {
    private static final long MAX_SHIFT_MS = 60_000;      // nadie puede tener el canal más de 1 minuto seguido
    private static final long SILENCE_MS = 3_000;        // si el que habla deja de mandar audio 3 s, se libera

    private record Member(WebSocketSession session, String name, String username, String role) {}

    private final Map<String, Member> members = new ConcurrentHashMap<>();
    private final UserAccountService users;
    private final ObjectMapper json = new ObjectMapper();

    private String talking;           // id de sesión que tiene el turno (null = canal libre)
    private long shiftSince, lastAudio;

    public RadioChannel(UserAccountService users) { this.users = users; }

    @Override
    public void afterConnectionEstablished(WebSocketSession s) throws Exception {
        if (s.getPrincipal() == null) { s.close(CloseStatus.POLICY_VIOLATION); return; }
        UserAccount u = users.byUsername(s.getPrincipal().getName()).orElse(null);
        if (u == null) { s.close(CloseStatus.POLICY_VIOLATION); return; }
        // El decorador evita que dos hilos escriban a la vez en la misma conexión
        WebSocketSession secure = new ConcurrentWebSocketSessionDecorator(s, 2000, 512 * 1024);
        members.put(s.getId(), new Member(secure, u.getName(), u.getUsername(), u.getRole().name()));
        send(secure, channelStatus());
        notifyPresent();
    }

    @Override
    protected void handleTextMessage(WebSocketSession s, TextMessage message) throws Exception {
        String type = json.readTree(message.getPayload()).path("t").asText();
        Member me = members.get(s.getId());
        if (me == null) return;
        synchronized (this) {
            switch (type) {
                case "talk" -> {
                    if (talking == null || talking.equals(s.getId())) {
                        talking = s.getId();
                        shiftSince = lastAudio = System.currentTimeMillis();
                        send(me.session(), Map.of("t", "granted"));
                        broadcast(Map.of("t", "speaking", "name", me.name(), "username", me.username(), "role", me.role()), s.getId());
                    } else {
                        send(me.session(), Map.of("t", "busy"));
                    }
                }
                case "release" -> { if (s.getId().equals(talking)) release(); }
                default -> { }
            }
        }
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession s, BinaryMessage audio) {
        synchronized (this) {
            if (!s.getId().equals(talking)) return;          // solo se reenvía la voz de quien tiene el turno
            lastAudio = System.currentTimeMillis();
        }
        byte[] data = new byte[audio.getPayloadLength()];
        audio.getPayload().get(data);
        for (Member m : members.values()) {
            if (m.session().getId().equals(s.getId())) continue;
            try { m.session().sendMessage(new BinaryMessage(data)); } catch (Exception ignored) { }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession s, CloseStatus status) {
        members.remove(s.getId());
        synchronized (this) { if (s.getId().equals(talking)) release(); }
        notifyPresent();
    }

    /** Si alguien se quedó con el canal (se le cayó el internet con el botón apretado), lo libera. */
    @Scheduled(fixedRate = 1000)
    public synchronized void watchShift() {
        if (talking == null) return;
        long now = System.currentTimeMillis();
        if (now - lastAudio > SILENCE_MS || now - shiftSince > MAX_SHIFT_MS) {
            Member m = members.get(talking);
            if (m != null) send(m.session(), Map.of("t", "cut"));
            release();
        }
    }

    // ---------- Internos ----------

    private void release() {
        talking = null;
        broadcast(Map.of("t", "free"), null);
    }

    private Map<String, Object> channelStatus() {
        Member m = talking == null ? null : members.get(talking);
        return m == null ? Map.of("t", "free") : Map.of("t", "speaking", "name", m.name(), "username", m.username(), "role", m.role());
    }

    /** Lista de quién está en el canal (una persona puede tener varias pantallas abiertas: se cuenta una vez). */
    private void notifyPresent() {
        Map<String, Map<String, String>> unique = new TreeMap<>();
        for (Member m : members.values()) unique.put(m.username(), Map.of("name", m.name(), "username", m.username(), "role", m.role()));
        broadcast(Map.of("t", "present", "list", new ArrayList<>(unique.values())), null);
    }

    private void broadcast(Object message, String except) {
        for (Member m : members.values()) if (!m.session().getId().equals(except)) send(m.session(), message);
    }

    private void send(WebSocketSession s, Object message) {
        try { s.sendMessage(new TextMessage(json.writeValueAsString(message))); } catch (IOException | IllegalStateException ignored) { }
    }
}
