package co.serviasigna.domain;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Forma de pago del cliente. En efectivo y transferencia el domiciliario registra lo recibido (RN-10).
 * <p>
 * Cada valor guarda su código en español para la base de datos ({@link #dbCode()}).
 * <p>
 * EN: Customer payment method. For cash and transfer the courier records the amount received (RN-10).
 * Each value keeps its Spanish database code ({@link #dbCode()}).
 */
public enum PaymentMethod implements DbCoded {

    /** Ya está pagado. EN: Already paid. */
    PREPAID("PREPAGADO"),

    /** Contra entrega en efectivo. EN: Cash on delivery. */
    CASH("EFECTIVO"),

    /** Contra entrega por transferencia. EN: Transfer on delivery. */
    TRANSFER("TRANSFERENCIA");

    /** Código en español que se guarda en la base de datos. EN: Spanish code stored in the database. */
    private final String dbCode;

    /**
     * Crea un valor con su código de base de datos.
     * <p>
     * EN: Creates a value with its database code.
     *
     * @param dbCode el código en español que se guarda en Oracle / the Spanish code stored in Oracle
     */
    PaymentMethod(String dbCode) {
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
    public static PaymentMethod fromJson(String value) {
        return DbCoded.fromDbCode(PaymentMethod.class, value);
    }

    /**
     * Indica si el dinero se cobra al entregar.
     * <p>
     * EN: Tells whether the money is collected on delivery.
     *
     * @return true si no es prepagado / true if it is not prepaid
     */
    public boolean cashOnDelivery() {
        return this != PREPAID;
    }
}
