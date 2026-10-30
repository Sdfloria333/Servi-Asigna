package co.serviasigna.domain;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Estados de una parada: pendiente, en camino, en el punto y completada, o fallida ante una novedad.
 * <p>
 * Cada valor guarda su código en español para la base de datos ({@link #dbCode()}).
 * <p>
 * EN: States of a stop: pending, on the way, at the stop and completed, or failed after an incident.
 * Each value keeps its Spanish database code ({@link #dbCode()}).
 */
public enum StopStatus implements DbCoded {

    /** Todavía no se ha empezado. EN: Not started yet. */
    PENDING("PENDIENTE"),

    /** El domiciliario va hacia el punto. EN: The courier is heading to the point. */
    ON_THE_WAY("EN_CAMINO"),

    /** El domiciliario llegó al punto. EN: The courier reached the point. */
    AT_STOP("EN_PUNTO"),

    /** La recogida o la entrega se confirmó. EN: The pickup or drop-off was confirmed. */
    COMPLETED("COMPLETADA"),

    /** No se pudo completar por una novedad. EN: Could not be completed because of an incident. */
    FAILED("FALLIDA");

    /** Código en español que se guarda en la base de datos. EN: Spanish code stored in the database. */
    private final String dbCode;

    /**
     * Crea un valor con su código de base de datos.
     * <p>
     * EN: Creates a value with its database code.
     *
     * @param dbCode el código en español que se guarda en Oracle / the Spanish code stored in Oracle
     */
    StopStatus(String dbCode) {
        this.dbCode = dbCode;
    }

    /**
     * Devuelve el código con el que este valor se guarda en la base de datos.
     * <p>
     * EN: Returns the code this value is stored with in the database.
     *
     * @return el código en español / the Spanish code
     */
    @Override
    public String dbCode() {
        return dbCode;
    }

    /**
     * Lee el valor que llega en un JSON: acepta el nombre en inglés o el código en español.
     * <p>
     * EN: Reads the value received in a JSON body: accepts the English name or the Spanish code.
     *
     * @param value el texto recibido / the received text
     * @return el valor encontrado / the matching value
     */
    @JsonCreator
    public static StopStatus fromJson(String value) {
        return DbCoded.fromDbCode(StopStatus.class, value);
    }
}
