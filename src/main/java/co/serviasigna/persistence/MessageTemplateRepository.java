package co.serviasigna.persistence;

import co.serviasigna.domain.MessageTemplate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acceso a la tabla PLANTILLA_MENSAJE: mensajes rápidos predefinidos.
 * <p>
 * Spring Data JPA escribe la implementación a partir del nombre de cada método: no hay que escribir SQL.
 * <p>
 * EN: Access to the PLANTILLA_MENSAJE table: predefined quick messages.
 * <p>
 * Spring Data JPA writes the implementation from each method name: no SQL needs to be written.
 */
public interface MessageTemplateRepository extends JpaRepository<MessageTemplate, Long> {
    /**
     * Trae las plantillas activas.
     * <p>
     * EN: Gets the active templates.
     *
     * @return las plantillas / the templates
     */
    List<MessageTemplate> findByActiveTrueOrderByIdAsc();
}
