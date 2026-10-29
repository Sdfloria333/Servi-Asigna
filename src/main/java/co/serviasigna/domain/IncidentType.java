package co.serviasigna.domain;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Catálogo de tipos de novedad. Cada tipo tiene el texto en español que se muestra en pantalla.
 * <p>
 * Cada valor guarda su código en español para la base de datos ({@link #dbCode()}).
 * <p>
 * EN: Catalog of incident types. Each type has the Spanish text shown on screen.
 * Each value keeps its Spanish database code ({@link #dbCode()}).
 */
public enum IncidentType implements DbCoded {

    /** El cliente no responde. EN: The customer does not answer. */
    CUSTOMER_NOT_RESPONDING("CLIENTE_NO_RESPONDE", "El cliente no responde"),

    /** Dirección errada. EN: Wrong address. */
    WRONG_ADDRESS("DIRECCION_ERRADA", "Dirección errada"),

    /** El pedido no está listo. EN: The order is not ready. */
    ORDER_NOT_READY("PEDIDO_NO_LISTO", "El pedido no está listo"),

    /** Pedido dañado. EN: Damaged order. */
    DAMAGED_ORDER("PEDIDO_DANADO", "Pedido dañado"),

    /** Accidente. EN: Accident. */
    ACCIDENT("ACCIDENTE", "Accidente"),

    /** Vehículo averiado. EN: Vehicle breakdown. */
    VEHICLE_BREAKDOWN("VEHICULO_AVERIADO", "Vehículo averiado"),

    /** Devolución del pedido. EN: Order return. */
    RETURN("DEVOLUCION", "Devolución del pedido"),

    /** Reclamo del cliente. EN: Customer complaint. */
    CUSTOMER_COMPLAINT("RECLAMO_CLIENTE", "Reclamo del cliente"),

    /** Otro. EN: Other. */
    OTHER("OTRO", "Otro");

    /** Código en español que se guarda en la base de datos. EN: Spanish code stored in the database. */
    private final String dbCode;
    /** Texto en español que se muestra en pantalla. EN: Spanish text shown on screen. */
    private final String text;

    /**
     * Crea un valor con su código de base de datos.
     * <p>
     * EN: Creates a value with its database code.
     *
     * @param dbCode el código en español que se guarda en Oracle / the Spanish code stored in Oracle
     * @param text texto en español que se muestra en pantalla / spanish text shown on screen
     */
    IncidentType(String dbCode, String text) {
        this.dbCode = dbCode;
        this.text = text;
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
    public static IncidentType fromJson(String value) {
        return DbCoded.fromDbCode(IncidentType.class, value);
    }

    /**
     * Devuelve el texto en español del tipo de novedad.
     * <p>
     * EN: Returns the Spanish text of the incident type.
     *
     * @return el texto para mostrar / the text to display
     */
    public String text() {
        return text;
    }
}
