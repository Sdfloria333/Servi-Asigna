package co.serviasigna.persistence;

import co.serviasigna.domain.PolicyAcceptance;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acceso a la tabla ACEPTACION_POLITICA: qué usuario aceptó qué versión de la política.
 * <p>
 * Spring Data JPA escribe la implementación a partir del nombre de cada método: no hay que escribir SQL.
 * <p>
 * EN: Access to the ACEPTACION_POLITICA table: which user accepted which policy version.
 * <p>
 * Spring Data JPA writes the implementation from each method name: no SQL needs to be written.
 */
public interface PolicyAcceptanceRepository extends JpaRepository<PolicyAcceptance, Long> {
    /**
     * Indica si un usuario ya aceptó una versión de la política.
     * <p>
     * EN: Tells whether a user already accepted a policy version.
     *
     * @param userId id del usuario / user id
     * @param policyId id de la política / policy id
     * @return true si ya la aceptó / true if already accepted
     */
    boolean existsByUserIdAndPolicyId(Long userId, Long policyId);
}
