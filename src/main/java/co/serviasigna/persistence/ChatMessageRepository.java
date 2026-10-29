package co.serviasigna.persistence;

import co.serviasigna.domain.ChatMessage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acceso a la tabla MENSAJE: conversación entre la central y cada domiciliario.
 * <p>
 * Spring Data JPA escribe la implementación a partir del nombre de cada método: no hay que escribir SQL.
 * <p>
 * EN: Access to the MENSAJE table: conversation between the dispatch office and each courier.
 * <p>
 * Spring Data JPA writes the implementation from each method name: no SQL needs to be written.
 */
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    /**
     * Trae los últimos 40 mensajes de la conversación con un domiciliario.
     * <p>
     * EN: Gets the last 40 messages of the conversation with a courier.
     *
     * @param courierId id del domiciliario / courier id
     * @return los mensajes, del más reciente al más antiguo / the messages, newest first
     */
    List<ChatMessage> findTop40ByCourierIdOrderByDateDesc(Long courierId);

    /**
     * Trae los mensajes que todavía no se han leído.
     * <p>
     * EN: Gets the messages that have not been read yet.
     *
     * @return los mensajes sin leer / the unread messages
     */
    List<ChatMessage> findByReadFalse();
}
