package co.serviasigna.persistence;

import co.serviasigna.domain.DeliveryProof;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acceso a la tabla EVIDENCIA_ENTREGA: pruebas de entrega (PIN, foto o firma).
 * <p>
 * Spring Data JPA escribe la implementación a partir del nombre de cada método: no hay que escribir SQL.
 * <p>
 * EN: Access to the EVIDENCIA_ENTREGA table: delivery proofs (PIN, photo or signature).
 * <p>
 * Spring Data JPA writes the implementation from each method name: no SQL needs to be written.
 */
public interface DeliveryProofRepository extends JpaRepository<DeliveryProof, Long> {
    /**
     * Trae las pruebas de entrega de un servicio.
     * <p>
     * EN: Gets the delivery proofs of a delivery.
     *
     * @param deliveryId id del servicio / delivery id
     * @return las pruebas / the proofs
     */
    List<DeliveryProof> findByDeliveryId(Long deliveryId);
}
