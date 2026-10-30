package co.serviasigna.domain;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Estado de una novedad reportada.
 * <p>
 * Cada valor guarda su código en español para la base de datos ({@link #dbCode()}).
 * <p>
 * EN: Status of a reported incident.
 * Each value keeps its Spanish database code ({@link #dbCode()}).
 */
public enum IncidentStatus implements DbCoded {

    /** Recién reportada. EN: Just reported. */
    OPEN("ABIERTO"),

    /** La central la está atendiendo. EN: The dispatch center is handling it. */
    IN_PROGRESS("EN_ATENCION"),

    /** Ya tiene una decisión. EN: It already has a decision. */
    CLOSED("CERRADO");

    /** Código en español que se guarda en la base de datos. EN: Spanish code stored in the database. */
    private final String dbCode;

    /**
     * Crea un valor con su código de base de datos.
     * <p>
     * EN: Creates a value with its database code.
     *
     * @param dbCode el código en español que se guarda en Oracle / the Spanish code stored in Oracle
     */
    IncidentStatus(String dbCode) {
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
    public static IncidentStatus fromJson(String value) {
        return DbCoded.fromDbCode(IncidentStatus.class, value);
    }
}
