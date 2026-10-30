package co.serviasigna.persistence;

import co.serviasigna.domain.IncidentStatus;
import co.serviasigna.domain.Incident;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acceso a la tabla INCIDENTE: novedades que reportan los domiciliarios.
 * <p>
 * Spring Data JPA escribe la implementación a partir del nombre de cada método: no hay que escribir SQL.
 * <p>
 * EN: Access to the INCIDENTE table: incidents reported by the couriers.
 * <p>
 * Spring Data JPA writes the implementation from each method name: no SQL needs to be written.
 */
public interface IncidentRepository extends JpaRepository<Incident, Long> {
    /**
     * Busca las novedades que no están en el estado dado (por ejemplo, las que no están cerradas).
     * <p>
     * EN: Finds the incidents that are not in the given status (for example, the ones not closed).
     *
     * @param status estado que se excluye / status to exclude
     * @return las novedades / the incidents
     */
    List<Incident> findByStatusNotOrderByIdDesc(IncidentStatus status);

    /**
     * Busca las novedades abiertas en un rango de fechas.
     * <p>
     * EN: Finds the incidents opened in a date range.
     *
     * @param from inicio del rango en minutos de reloj / range start in clock minutes
     * @param to fin del rango en minutos de reloj / range end in clock minutes
     * @return las novedades / the incidents
     */
    List<Incident> findByOpenedBetweenOrderByIdDesc(Double from, Double to);

    /**
     * Busca las novedades de un servicio.
     * <p>
     * EN: Finds the incidents of a delivery.
     *
     * @param deliveryId id del servicio / delivery id
     * @return las novedades / the incidents
     */
    List<Incident> findByDeliveryIdOrderByIdAsc(Long deliveryId);
}
