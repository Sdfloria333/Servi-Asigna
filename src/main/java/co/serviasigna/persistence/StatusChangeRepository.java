package co.serviasigna.persistence;

import co.serviasigna.domain.StatusChange;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acceso a la tabla HISTORIAL_ESTADO_SERVICIO: cada cambio de estado de cada servicio (RN-01).
 * <p>
 * Spring Data JPA escribe la implementación a partir del nombre de cada método: no hay que escribir SQL.
 * <p>
 * EN: Access to the HISTORIAL_ESTADO_SERVICIO table: every status change of every delivery (RN-01).
 * <p>
 * Spring Data JPA writes the implementation from each method name: no SQL needs to be written.
 */
public interface StatusChangeRepository extends JpaRepository<StatusChange, Long> {
    /**
     * Trae el historial de un servicio en orden cronológico.
     * <p>
     * EN: Gets the history of a delivery in chronological order.
     *
     * @param deliveryId id del servicio / delivery id
     * @return los cambios de estado / the status changes
     */
    List<StatusChange> findByDeliveryIdOrderByDateAscIdAsc(Long deliveryId);
}
