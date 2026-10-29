package co.serviasigna.persistence;

import co.serviasigna.domain.IncidentFollowUp;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acceso a la tabla SEGUIMIENTO_INCIDENTE: comentarios y decisiones sobre cada novedad.
 * <p>
 * Spring Data JPA escribe la implementación a partir del nombre de cada método: no hay que escribir SQL.
 * <p>
 * EN: Access to the SEGUIMIENTO_INCIDENTE table: comments and decisions about each incident.
 * <p>
 * Spring Data JPA writes the implementation from each method name: no SQL needs to be written.
 */
public interface IncidentFollowUpRepository extends JpaRepository<IncidentFollowUp, Long> {
    /**
     * Trae el seguimiento de una novedad, en orden.
     * <p>
     * EN: Gets the follow-up of an incident, in order.
     *
     * @param incidentId id de la novedad / incident id
     * @return el seguimiento / the follow-up
     */
    List<IncidentFollowUp> findByIncidentIdOrderByDateAsc(Long incidentId);
}
