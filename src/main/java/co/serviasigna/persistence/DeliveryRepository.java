package co.serviasigna.persistence;

import co.serviasigna.domain.DeliveryStatus;
import co.serviasigna.domain.Delivery;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acceso a la tabla SERVICIO: los pedidos de recogida y entrega.
 * <p>
 * Spring Data JPA escribe la implementación a partir del nombre de cada método: no hay que escribir SQL.
 * <p>
 * EN: Access to the SERVICIO table: the pickup and drop-off orders.
 * <p>
 * Spring Data JPA writes the implementation from each method name: no SQL needs to be written.
 */
public interface DeliveryRepository extends JpaRepository<Delivery, Long> {
    /**
     * Busca los servicios que están en alguno de los estados dados (al arrancar se cargan los abiertos).
     * <p>
     * EN: Finds the deliveries in any of the given statuses (the open ones are loaded at startup).
     *
     * @param statuses estados buscados / wanted statuses
     * @return los servicios / the deliveries
     */
    List<Delivery> findByStatusIn(Collection<DeliveryStatus> statuses);

    /**
     * Busca los servicios creados en un rango de fechas (búsqueda histórica y reportes).
     * <p>
     * EN: Finds the deliveries created in a date range (history search and reports).
     *
     * @param from inicio del rango en minutos de reloj / range start in clock minutes
     * @param to fin del rango en minutos de reloj / range end in clock minutes
     * @return los servicios, del más nuevo al más viejo / the deliveries, newest first
     */
    List<Delivery> findByCreatedBetweenOrderByIdDesc(Double from, Double to);

    /**
     * Busca los servicios de un cliente.
     * <p>
     * EN: Finds the deliveries of a customer.
     *
     * @param customerId id del cliente / customer id
     * @return los servicios, del más nuevo al más viejo / the deliveries, newest first
     */
    List<Delivery> findByCustomerIdOrderByIdDesc(Long customerId);
}
