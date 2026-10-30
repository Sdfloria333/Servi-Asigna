package co.serviasigna.persistence;

import co.serviasigna.domain.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acceso a la tabla EVENTO_PROCESADO: eventos sin conexión ya aplicados, para no repetirlos (RF-M6-11).
 * <p>
 * Spring Data JPA escribe la implementación a partir del nombre de cada método: no hay que escribir SQL.
 * <p>
 * EN: Access to the EVENTO_PROCESADO table: offline events already applied, so they are not repeated (RF-M6-11).
 * <p>
 * Spring Data JPA writes the implementation from each method name: no SQL needs to be written.
 */
public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, String> {}
