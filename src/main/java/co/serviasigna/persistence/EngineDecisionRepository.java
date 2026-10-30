package co.serviasigna.persistence;

import co.serviasigna.domain.EngineDecision;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acceso a la tabla DECISION_MOTOR: por qué el motor eligió a cada domiciliario (RF-M4-07).
 * <p>
 * Spring Data JPA escribe la implementación a partir del nombre de cada método: no hay que escribir SQL.
 * <p>
 * EN: Access to the DECISION_MOTOR table: why the engine chose each courier (RF-M4-07).
 * <p>
 * Spring Data JPA writes the implementation from each method name: no SQL needs to be written.
 */
public interface EngineDecisionRepository extends JpaRepository<EngineDecision, Long> {
    /**
     * Trae las decisiones del motor sobre un servicio, en orden.
     * <p>
     * EN: Gets the engine decisions about a delivery, in order.
     *
     * @param deliveryId id del servicio / delivery id
     * @return las decisiones / the decisions
     */
    List<EngineDecision> findByDeliveryIdOrderByDateAsc(Long deliveryId);
}
