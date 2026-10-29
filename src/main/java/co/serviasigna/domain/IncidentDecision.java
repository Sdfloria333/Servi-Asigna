package co.serviasigna.domain;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Lo que decide la central ante una novedad.
 * <p>
 * Cada valor guarda su código en español para la base de datos ({@link #dbCode()}).
 * <p>
 * EN: What the dispatch center decides about an incident.
 * Each value keeps its Spanish database code ({@link #dbCode()}).
 */
public enum IncidentDecision implements DbCoded {

    /** Volver a intentar la entrega. EN: Try the delivery again. */
    RETRY("REINTENTAR"),

    /** Devolver el servicio a la cola. EN: Send the delivery back to the queue. */
    RELEASE("LIBERAR"),

    /** Cancelar el servicio. EN: Cancel the delivery. */
    CANCEL("CANCELAR"),

    /** Marcar el servicio como fallido. EN: Mark the delivery as failed. */
    MARK_FAILED("MARCAR_FALLIDO"),

    /** Cerrar la novedad sin cambiar el servicio. EN: Close the incident without changing the delivery. */
    NO_ACTION("SIN_ACCION");

    /** Código en español que se guarda en la base de datos. EN: Spanish code stored in the database. */
    private final String dbCode;

    /**
     * Crea un valor con su código de base de datos.
     * <p>
     * EN: Creates a value with its database code.
     *
     * @param dbCode el código en español que se guarda en Oracle / the Spanish code stored in Oracle
     */
    IncidentDecision(String dbCode) {
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
    public static IncidentDecision fromJson(String value) {
        return DbCoded.fromDbCode(IncidentDecision.class, value);
    }
}
