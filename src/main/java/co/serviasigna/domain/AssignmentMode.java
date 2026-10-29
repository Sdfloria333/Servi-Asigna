package co.serviasigna.domain;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Modos del motor de asignación.
 * <p>
 * Cada valor guarda su código en español para la base de datos ({@link #dbCode()}).
 * <p>
 * EN: Assignment engine modes.
 * Each value keeps its Spanish database code ({@link #dbCode()}).
 */
public enum AssignmentMode implements DbCoded {

    /** La central asigna cada servicio. EN: The dispatch center assigns every delivery. */
    MANUAL("MANUAL"),

    /** Se ofrece a varios y gana el primero que acepte. EN: Offered to several couriers; the first to accept wins. */
    BROADCAST("DIFUSION"),

    /** El motor elige al mejor domiciliario y se lo ofrece. EN: The engine picks the best courier and offers it. */
    AUTOMATIC("AUTOMATICO");

    /** Código en español que se guarda en la base de datos. EN: Spanish code stored in the database. */
    private final String dbCode;

    /**
     * Crea un valor con su código de base de datos.
     * <p>
     * EN: Creates a value with its database code.
     *
     * @param dbCode el código en español que se guarda en Oracle / the Spanish code stored in Oracle
     */
    AssignmentMode(String dbCode) {
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
    public static AssignmentMode fromJson(String value) {
        return DbCoded.fromDbCode(AssignmentMode.class, value);
    }
}
