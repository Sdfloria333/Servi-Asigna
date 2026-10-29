package co.serviasigna.realtime;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Tiempo real con STOMP sobre WebSocket (RF-M9-01, RF-M9-02).
 *   ws://servidor/ws/stomp           punto de conexión (exige sesión: pasa por SecurityConfig)
 *   /topic/central                   estado completo de la operación (solo ADMIN y DESPACHADOR)
 *   /topic/central/avisos            avisos para la central: novedades, mensajes, falla del motor
 *   /user/queue/avisos               avisos para UN domiciliario: oferta, asignado, revocado, mensaje...
 * La radio de voz sigue en /ws/radio (WebSocket binario aparte, RadioConfig).
 */
@Configuration
@EnableWebSocketMessageBroker
public class StompConfig implements WebSocketMessageBrokerConfigurer {
    private final SubscriptionInterceptor interceptor;

    public StompConfig(SubscriptionInterceptor interceptor) { this.interceptor = interceptor; }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registration) {
        registration.addEndpoint("/ws/stomp");
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registration) {
        registration.enableSimpleBroker("/topic", "/queue")
                .setHeartbeatValue(new long[] { 10_000, 10_000 })
                .setTaskScheduler(taskScheduler());
        registration.setApplicationDestinationPrefixes("/app");
        registration.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(interceptor);
    }

    /** Latidos cada 10 s: mantienen viva la conexión aunque no haya cambios (y detectan celulares que se cayeron). */
    @Bean
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler t = new ThreadPoolTaskScheduler();
        t.setPoolSize(4);                 // también lo usan las tareas @Scheduled (motor cada 1 s, envío de estado)
        t.setThreadNamePrefix("tareas-");
        t.initialize();
        return t;
    }
}
