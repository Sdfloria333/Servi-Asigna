package co.serviasigna.persistence;

import co.serviasigna.domain.DeliveryStop;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acceso a la tabla PARADA: la parada de recogida y la de entrega de cada servicio.
 * <p>
 * Spring Data JPA escribe la implementación a partir del nombre de cada método: no hay que escribir SQL.
 * <p>
 * EN: Access to the PARADA table: the pickup stop and the drop-off stop of each delivery.
 * <p>
 * Spring Data JPA writes the implementation from each method name: no SQL needs to be written.
 */
public interface DeliveryStopRepository extends JpaRepository<DeliveryStop, Long> {
    /**
     * Trae las paradas de varios servicios a la vez.
     * <p>
     * EN: Gets the stops of several deliveries at once.
     *
     * @param deliveries ids de los servicios / delivery ids
     * @return las paradas / the stops
     */
    List<DeliveryStop> findByDeliveryIdIn(Collection<Long> deliveries);

    /**
     * Trae las paradas de un servicio.
     * <p>
     * EN: Gets the stops of a delivery.
     *
     * @param deliveryId id del servicio / delivery id
     * @return las paradas / the stops
     */
    List<DeliveryStop> findByDeliveryId(Long deliveryId);
}
