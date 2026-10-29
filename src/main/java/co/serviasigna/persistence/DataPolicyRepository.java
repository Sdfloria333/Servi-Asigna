package co.serviasigna.persistence;

import co.serviasigna.domain.DataPolicy;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acceso a la tabla POLITICA_DATOS: versiones de la política de tratamiento de datos.
 * <p>
 * Spring Data JPA escribe la implementación a partir del nombre de cada método: no hay que escribir SQL.
 * <p>
 * EN: Access to the POLITICA_DATOS table: versions of the data processing policy.
 * <p>
 * Spring Data JPA writes the implementation from each method name: no SQL needs to be written.
 */
public interface DataPolicyRepository extends JpaRepository<DataPolicy, Long> {
    /**
     * Busca la política vigente más reciente.
     * <p>
     * EN: Finds the most recent current policy.
     *
     * @return la política vigente, si hay / the current policy, if any
     */
    Optional<DataPolicy> findFirstByCurrentTrueOrderByIdDesc();

    /**
     * Trae todas las políticas marcadas como vigentes (para desmarcarlas al publicar una nueva).
     * <p>
     * EN: Gets every policy marked as current (to unmark them when a new one is published).
     *
     * @return las políticas vigentes / the current policies
     */
    List<DataPolicy> findByCurrentTrue();
}
