package co.serviasigna.domain;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Estado de una cuenta de usuario. Las cuentas no se borran: se cambia su estado (borrado lógico).
 * <p>
 * Cada valor guarda su código en español para la base de datos ({@link #dbCode()}).
 * <p>
 * EN: Status of a user account. Accounts are never deleted: their status changes instead (soft delete).
 * Each value keeps its Spanish database code ({@link #dbCode()}).
 */
public enum AccountStatus implements DbCoded {

    /** Se registró y espera aprobación. EN: Registered and waiting for approval. */
    PENDING("PENDIENTE"),

    /** Puede entrar al sistema. EN: Can log in. */
    ACTIVE("ACTIVO"),

    /** Desactivada por el administrador. EN: Deactivated by the administrator. */
    INACTIVE("INACTIVO"),

    /** Suspendida temporalmente. EN: Temporarily suspended. */
    SUSPENDED("SUSPENDIDO"),

    /** Solicitud de registro negada. EN: Registration request denied. */
    REJECTED("RECHAZADO");

    /** Código en español que se guarda en la base de datos. EN: Spanish code stored in the database. */
    private final String dbCode;

    /**
     * Crea un valor con su código de base de datos.
     * <p>
     * EN: Creates a value with its database code.
     *
     * @param dbCode el código en español que se guarda en Oracle / the Spanish code stored in Oracle
     */
    AccountStatus(String dbCode) {
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
    public static AccountStatus fromJson(String value) {
        return DbCoded.fromDbCode(AccountStatus.class, value);
    }
}
