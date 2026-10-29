package co.serviasigna.domain;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Resultado de una oferta enviada a un domiciliario (RN-05).
 * <p>
 * Cada valor guarda su código en español para la base de datos ({@link #dbCode()}).
 * <p>
 * EN: Outcome of an offer sent to a courier (RN-05).
 * Each value keeps its Spanish database code ({@link #dbCode()}).
 */
public enum OfferStatus implements DbCoded {

    /** Espera respuesta. EN: Waiting for an answer. */
    PENDING("PENDIENTE"),

    /** El domiciliario la aceptó. EN: The courier accepted it. */
    ACCEPTED("ACEPTADA"),

    /** El domiciliario la rechazó. EN: The courier rejected it. */
    REJECTED("RECHAZADA"),

    /** Se acabó el tiempo sin respuesta. EN: Time ran out without an answer. */
    EXPIRED("VENCIDA"),

    /** Se anuló porque otro la tomó o el servicio cambió. EN: Voided because someone else took it or the delivery changed. */
    VOIDED("ANULADA");

    /** Código en español que se guarda en la base de datos. EN: Spanish code stored in the database. */
    private final String dbCode;

    /**
     * Crea un valor con su código de base de datos.
     * <p>
     * EN: Creates a value with its database code.
     *
     * @param dbCode el código en español que se guarda en Oracle / the Spanish code stored in Oracle
     */
    OfferStatus(String dbCode) {
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
    public static OfferStatus fromJson(String value) {
        return DbCoded.fromDbCode(OfferStatus.class, value);
    }
}
