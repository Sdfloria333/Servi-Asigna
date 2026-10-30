package co.serviasigna.domain;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Pruebas de entrega que puede dejar el domiciliario.
 * <p>
 * Cada valor guarda su código en español para la base de datos ({@link #dbCode()}).
 * <p>
 * EN: Proofs of delivery the courier can leave.
 * Each value keeps its Spanish database code ({@link #dbCode()}).
 */
public enum ProofType implements DbCoded {

    /** PIN de 4 dígitos que dicta el cliente. EN: 4-digit PIN given by the customer. */
    PIN("PIN"),

    /** Foto de la entrega. EN: Photo of the delivery. */
    PHOTO("FOTO"),

    /** Firma del cliente. EN: Customer signature. */
    SIGNATURE("FIRMA");

    /** Código en español que se guarda en la base de datos. EN: Spanish code stored in the database. */
    private final String dbCode;

    /**
     * Crea un valor con su código de base de datos.
     * <p>
     * EN: Creates a value with its database code.
     *
     * @param dbCode el código en español que se guarda en Oracle / the Spanish code stored in Oracle
     */
    ProofType(String dbCode) {
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
    public static ProofType fromJson(String value) {
        return DbCoded.fromDbCode(ProofType.class, value);
    }
}
