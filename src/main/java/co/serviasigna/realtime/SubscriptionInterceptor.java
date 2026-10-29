package co.serviasigna.realtime;

import java.security.Principal;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Nadie puede escuchar lo que no le corresponde: /topic/central* solo la central; nadie publica directo a /topic o /queue.
 * Los avisos personales (/user/queue/avisos) Spring los entrega solo a la sesión de ese usuario.
 */
@Component
public class SubscriptionInterceptor implements ChannelInterceptor {

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor h = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (h == null || h.getCommand() == null) return message;
        Principal p = h.getUser();
        if (h.getCommand() == StompCommand.CONNECT && p == null) throw new IllegalStateException("Debes iniciar sesión");
        String destination = h.getDestination() == null ? "" : h.getDestination();
        if (h.getCommand() == StompCommand.SUBSCRIBE && destination.startsWith("/topic/central") && !isCentral(p))
            throw new IllegalStateException("Solo la central puede suscribirse a " + destination);
        if (h.getCommand() == StompCommand.SEND && (destination.startsWith("/topic") || destination.startsWith("/queue") || destination.startsWith("/user")))
            throw new IllegalStateException("No se puede publicar en " + destination);
        return message;
    }

    static boolean isCentral(Principal p) {
        return p instanceof Authentication a && a.getAuthorities().stream()
                .anyMatch(g -> g.getAuthority().equals("ROLE_ADMIN") || g.getAuthority().equals("ROLE_DISPATCHER"));
    }
}
