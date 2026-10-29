package co.serviasigna.domain;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Estado del domiciliario en la aplicación. Lo cambia él mismo desde la app; EN_RUTA lo pone el sistema cuando tiene paradas.
 * <p>
 * Cada valor guarda su código en español para la base de datos ({@link #dbCode()}).
 * <p>
 * EN: Courier status in the app. The courier changes it from the app; ON_ROUTE is set by the system when there are stops.
 * Each value keeps its Spanish database code ({@link #dbCode()}).
 */
public enum CourierStatus implements DbCoded {

    /** No trabaja: no recibe ofertas y no se registra su ubicación. EN: Not working: receives no offers and the location is not recorded. */
    OFF_DUTY("FUERA_DE_SERVICIO"),

    /** Recibe ofertas. EN: Receives offers. */
    AVAILABLE("DISPONIBLE"),

    /** En pausa: conserva su ruta pero no recibe ofertas nuevas. EN: Paused: keeps the route but receives no new offers. */
    BUSY("OCUPADO"),

    /** Tiene paradas y solo recibe ofertas compatibles con su ruta. EN: Has stops and only receives offers compatible with the route. */
    ON_ROUTE("EN_RUTA");

    /** Código en español que se guarda en la base de datos. EN: Spanish code stored in the database. */
    private final String dbCode;

    /**
     * Crea un valor con su código de base de datos.
     * <p>
     * EN: Creates a value with its database code.
     *
     * @param dbCode el código en español que se guarda en Oracle / the Spanish code stored in Oracle
     */
    CourierStatus(String dbCode) {
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
    public static CourierStatus fromJson(String value) {
        return DbCoded.fromDbCode(CourierStatus.class, value);
    }

    /**
     * Indica si el domiciliario está trabajando.
     * <p>
     * EN: Tells whether the courier is working.
     *
     * @return true si no está fuera de servicio / true if not off duty
     */
    public boolean working() {
        return this != OFF_DUTY;
    }
}
