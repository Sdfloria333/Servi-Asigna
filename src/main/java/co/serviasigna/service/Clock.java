package co.serviasigna.service;

import org.springframework.stereotype.Component;

/**
 * La hora real. El sistema trabaja en "minutos de reloj" (minutos desde 1970) porque así el motor
 * suma y resta minutos fácil; la pantalla los convierte a la hora local (14:35).
 */
@Component
public class Clock {
    public double now() { return System.currentTimeMillis() / 60000.0; }
}
