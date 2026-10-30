package co.serviasigna.persistence;

import co.serviasigna.domain.AuditEntry;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acceso a la tabla AUDITORIA: registro de las acciones importantes (RNF-06).
 * <p>
 * Spring Data JPA escribe la implementación a partir del nombre de cada método: no hay que escribir SQL.
 * <p>
 * EN: Access to the AUDITORIA table: log of the important actions (RNF-06).
 * <p>
 * Spring Data JPA writes the implementation from each method name: no SQL needs to be written.
 */
public interface AuditEntryRepository extends JpaRepository<AuditEntry, Long> {
    /**
     * Busca las acciones de un rango de fechas, de la más reciente a la más antigua.
     * <p>
     * EN: Finds the actions in a date range, newest first.
     *
     * @param from inicio del rango en minutos de reloj / range start in clock minutes
     * @param to fin del rango en minutos de reloj / range end in clock minutes
     * @return las acciones del rango / the actions in the range
     */
    List<AuditEntry> findByDateBetweenOrderByDateDesc(Double from, Double to);

    /**
     * Cuenta las intervenciones manuales de la central en un rango (indicador I7).
     * <p>
     * EN: Counts the manual interventions of the dispatch office in a range (indicator I7).
     *
     * @param from inicio del rango en minutos de reloj / range start in clock minutes
     * @param to fin del rango en minutos de reloj / range end in clock minutes
     * @return cuántas intervenciones hubo / how many interventions there were
     */
    long countByManualInterventionTrueAndDateBetween(Double from, Double to);
}
