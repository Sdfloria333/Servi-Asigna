package co.serviasigna.domain;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Estados de un servicio de domicilio. Las transiciones permitidas entre ellos las valida {@link DeliveryStateMachine} (RN-01).
 * <p>
 * Cada valor guarda su código en español para la base de datos ({@link #dbCode()}).
 * <p>
 * EN: States of a delivery. The allowed transitions between them are validated by {@link DeliveryStateMachine} (RN-01).
 * Each value keeps its Spanish database code ({@link #dbCode()}).
 */
public enum DeliveryStatus implements DbCoded {

    /** Recién creado por la central. EN: Just created by the dispatch center. */
    CREATED("CREADO"),

    /** Entra a la cola a una hora futura. EN: Joins the queue at a future time. */
    SCHEDULED("PROGRAMADO"),

    /** Espera un domiciliario. EN: Waiting for a courier. */
    QUEUED("EN_COLA"),

    /** Se ofreció a uno o varios domiciliarios y corre el temporizador. EN: Offered to one or more couriers while the timer runs. */
    OFFERED("OFERTADO"),

    /** Un domiciliario lo aceptó y aún no lo recoge. EN: A courier accepted it and has not picked it up yet. */
    ASSIGNED("ASIGNADO"),

    /** Ya se recogió y va camino a la entrega. EN: Already picked up and on the way to the drop-off. */
    IN_PROGRESS("EN_EJECUCION"),

    /** Final: entregado con prueba de entrega. EN: Final: delivered with proof of delivery. */
    DELIVERED("ENTREGADO"),

    /** No se pudo entregar; la central puede reintentarlo. EN: Could not be delivered; the dispatch center may retry it. */
    FAILED("FALLIDO"),

    /** Final: cancelado con motivo. EN: Final: cancelled with a reason. */
    CANCELLED("CANCELADO");

    /** Código en español que se guarda en la base de datos. EN: Spanish code stored in the database. */
    private final String dbCode;

    /**
     * Crea un valor con su código de base de datos.
     * <p>
     * EN: Creates a value with its database code.
     *
     * @param dbCode el código en español que se guarda en Oracle / the Spanish code stored in Oracle
     */
    DeliveryStatus(String dbCode) {
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
    public static DeliveryStatus fromJson(String value) {
        return DbCoded.fromDbCode(DeliveryStatus.class, value);
    }

    /**
     * Indica si el servicio sigue en la operación, es decir, si todavía no terminó.
     * <p>
     * EN: Tells whether the delivery is still in operation, that is, not finished yet.
     *
     * @return true si no está entregado, cancelado ni fallido / true if it is not delivered, cancelled or failed
     */
    public boolean isOpen() {
        return this != DELIVERED && this != CANCELLED && this != FAILED;
    }

    /**
     * Indica si un domiciliario ya está trabajando el servicio.
     * <p>
     * EN: Tells whether a courier is already working on the delivery.
     *
     * @return true si está asignado o en ejecución / true if it is assigned or in progress
     */
    public boolean withCourier() {
        return this == ASSIGNED || this == IN_PROGRESS;
    }
}
