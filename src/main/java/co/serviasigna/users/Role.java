package co.serviasigna.users;

import co.serviasigna.domain.DbCoded;
import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Roles de las cuentas del sistema.
 * <p>
 * Cada valor guarda su código en español para la base de datos ({@link #dbCode()}).
 * <p>
 * EN: Account roles of the system.
 * Each value keeps its Spanish database code ({@link #dbCode()}).
 */
public enum Role implements DbCoded {

    /** Configura el sistema (usuarios, parámetros, tarifa, política) y también opera. EN: Configures the system (users, settings, rate, policy) and also operates. */
    ADMIN("ADMIN"),

    /** Opera el día a día: servicios, flota y novedades. EN: Runs daily operations: deliveries, fleet and incidents. */
    DISPATCHER("DESPACHADOR"),

    /** Usa la app del celular. EN: Uses the mobile app. */
    COURIER("DOMICILIARIO");

    /** Código en español que se guarda en la base de datos. EN: Spanish code stored in the database. */
    private final String dbCode;

    /**
     * Crea un valor con su código de base de datos.
     * <p>
     * EN: Creates a value with its database code.
     *
     * @param dbCode el código en español que se guarda en Oracle / the Spanish code stored in Oracle
     */
    Role(String dbCode) {
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
    public static Role fromJson(String value) {
        return DbCoded.fromDbCode(Role.class, value);
    }

    /**
     * Indica si el rol pertenece a la central (administrador o despachador).
     * <p>
     * EN: Tells whether the role belongs to the dispatch center (admin or dispatcher).
     *
     * @return true si no es domiciliario / true if it is not a courier
     */
    public boolean isCentral() {
        return this != COURIER;
    }
}
