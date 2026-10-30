package co.serviasigna.radio;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

/** Publica la radio en ws://servidor/ws/radio (solo para quien inició sesión, ver SecurityConfig). */
@Configuration
@EnableWebSocket
public class RadioConfig implements WebSocketConfigurer {
    private final RadioChannel channel;

    public RadioConfig(RadioChannel channel) { this.channel = channel; }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registration) {
        registration.addHandler(channel, "/ws/radio");
    }

    /** Pedazos de audio de hasta 64 KB (cada uno trae ~0,1 s de voz, unos 3 KB). */
    @Bean
    public ServletServerContainerFactoryBean webSocketContainer() {
        ServletServerContainerFactoryBean c = new ServletServerContainerFactoryBean();
        c.setMaxBinaryMessageBufferSize(64 * 1024);
        c.setMaxTextMessageBufferSize(16 * 1024);
        return c;
    }
}
