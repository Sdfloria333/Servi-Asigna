package co.serviasigna.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.sql.Timestamp;

/**
 * Convierte las horas entre memoria y Oracle.
 * <p>
 * En memoria las horas son "minutos de reloj" (minutos desde 1970) porque el motor suma y resta minutos. En Oracle
 * se guardan como TIMESTAMP con la hora de Bogotá (RN-17; ver hibernate.jdbc.time_zone).
 * <p>
 * EN: Converts times between memory and Oracle.
 * <p>
 * In memory, times are "clock minutes" (minutes since 1970) because the engine adds and subtracts minutes. In
 * Oracle they are stored as TIMESTAMP in Bogotá time (RN-17; see hibernate.jdbc.time_zone).
 */
@Converter
public class MinutesConverter implements AttributeConverter<Double, Timestamp> {
    /**
     * Convierte minutos de reloj en un TIMESTAMP para guardar.
     * <p>
     * EN: Turns clock minutes into a TIMESTAMP to store.
     *
     * @param minutes minutos desde 1970, puede ser null / minutes since 1970, may be null
     * @return la fecha o null / the date or null
     */
    @Override public Timestamp convertToDatabaseColumn(Double minutes) {
        return minutes == null ? null : new Timestamp(Math.round(minutes * 60000));
    }

    /**
     * Convierte el TIMESTAMP leído en minutos de reloj.
     * <p>
     * EN: Turns the TIMESTAMP read into clock minutes.
     *
     * @param t fecha leída, puede ser null / date read, may be null
     * @return los minutos o null / the minutes or null
     */
    @Override public Double convertToEntityAttribute(Timestamp t) {
        return t == null ? null : t.getTime() / 60000.0;
    }
}
