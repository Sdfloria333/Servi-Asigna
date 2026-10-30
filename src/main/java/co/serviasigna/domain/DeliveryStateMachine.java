package co.serviasigna.domain;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static co.serviasigna.domain.DeliveryStatus.*;

/**
 * Transiciones permitidas entre los estados de un servicio (sección 6.1 del documento, RN-11): no se puede saltar
 * estados.
 * <p>
 * ASSIGNED → ASSIGNED también está permitido: es una reasignación a otro domiciliario.
 * <p>
 * EN: Allowed transitions between the statuses of a delivery (section 6.1 of the document, RN-11): statuses cannot be
 * skipped.
 * <p>
 * ASSIGNED → ASSIGNED is also allowed: it is a reassignment to another courier.
 */
public final class DeliveryStateMachine {
    /** Para cada estado, los estados a los que puede pasar. EN: For each status, the statuses it can move to. */
    private static final Map<DeliveryStatus, Set<DeliveryStatus>> ALLOWED = new EnumMap<>(DeliveryStatus.class);
    // Tabla de transiciones (sección 6.1). EN: Transition table (section 6.1).
    static {
        ALLOWED.put(CREATED,       EnumSet.of(QUEUED, SCHEDULED, CANCELLED));
        ALLOWED.put(SCHEDULED,   EnumSet.of(QUEUED, CANCELLED));
        ALLOWED.put(QUEUED,      EnumSet.of(OFFERED, ASSIGNED, CANCELLED));
        ALLOWED.put(OFFERED,     EnumSet.of(ASSIGNED, QUEUED, CANCELLED));
        ALLOWED.put(ASSIGNED,     EnumSet.of(IN_PROGRESS, QUEUED, CANCELLED, FAILED, ASSIGNED));   // ASIGNADO→ASIGNADO = reasignación
        ALLOWED.put(IN_PROGRESS, EnumSet.of(DELIVERED, FAILED, CANCELLED));
        ALLOWED.put(FAILED,      EnumSet.of(QUEUED, CANCELLED));
        ALLOWED.put(DELIVERED,    EnumSet.noneOf(DeliveryStatus.class));
        ALLOWED.put(CANCELLED,    EnumSet.noneOf(DeliveryStatus.class));
    }

    /**
     * Evita crear instancias: la clase solo tiene métodos estáticos.
     * <p>
     * EN: Prevents instantiation: the class only has static methods.
     */
    private DeliveryStateMachine() {}

    /**
     * Indica si se puede pasar de un estado a otro.
     * <p>
     * EN: Tells whether a status can move to another one.
     *
     * @param from estado actual / current status
     * @param towards estado al que se quiere pasar / status to move to
     * @return true si la transición existe / true if the transition exists
     */
    public static boolean allowed(DeliveryStatus from, DeliveryStatus towards) {
        return ALLOWED.get(from).contains(towards);
    }

    /**
     * Devuelve los estados a los que se puede pasar desde un estado.
     * <p>
     * EN: Returns the statuses that can follow a status.
     *
     * @param from estado actual / current status
     * @return los estados siguientes / the following statuses
     */
    public static Set<DeliveryStatus> following(DeliveryStatus from) { return ALLOWED.get(from); }

    /**
     * Revisa una transición y lanza un error legible si no existe.
     * <p>
     * EN: Checks a transition and throws a readable error if it does not exist.
     *
     * @param from estado actual / current status
     * @param towards estado al que se quiere pasar / status to move to
     * @throws IllegalStateException si la transición no está permitida / if the transition is not allowed
     */
    public static void validate(DeliveryStatus from, DeliveryStatus towards) {
        if (!allowed(from, towards))
            throw new IllegalStateException("No se puede pasar un servicio de " + text(from) + " a " + text(towards));
    }

    /**
     * Devuelve el nombre del estado en español, para los mensajes de error.
     * <p>
     * EN: Returns the Spanish name of the status, for error messages.
     *
     * @param e estado / status
     * @return el nombre en español / the Spanish name
     */
    public static String text(DeliveryStatus e) {
        return switch (e) {
            case CREATED -> "Creado"; case SCHEDULED -> "Programado"; case QUEUED -> "Sin asignar"; case OFFERED -> "Ofertado";
            case ASSIGNED -> "Asignado"; case IN_PROGRESS -> "En camino"; case DELIVERED -> "Entregado";
            case FAILED -> "Fallido"; case CANCELLED -> "Cancelado";
        };
    }
}
