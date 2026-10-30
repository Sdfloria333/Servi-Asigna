package co.serviasigna.persistence;

import co.serviasigna.domain.OfferStatus;
import co.serviasigna.domain.Offer;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acceso a la tabla OFERTA: ofertas de servicios enviadas a los domiciliarios.
 * <p>
 * Spring Data JPA escribe la implementación a partir del nombre de cada método: no hay que escribir SQL.
 * <p>
 * EN: Access to the OFERTA table: delivery offers sent to the couriers.
 * <p>
 * Spring Data JPA writes the implementation from each method name: no SQL needs to be written.
 */
public interface OfferRepository extends JpaRepository<Offer, Long> {
    /**
     * Busca las ofertas en un estado (al arrancar se cargan las pendientes).
     * <p>
     * EN: Finds the offers in a status (the pending ones are loaded at startup).
     *
     * @param status estado buscado / wanted status
     * @return las ofertas / the offers
     */
    List<Offer> findByStatus(OfferStatus status);

    /**
     * Trae las ofertas de un servicio en el orden en que se enviaron.
     * <p>
     * EN: Gets the offers of a delivery in the order they were sent.
     *
     * @param deliveryId id del servicio / delivery id
     * @return las ofertas / the offers
     */
    List<Offer> findByDeliveryIdOrderBySentAsc(Long deliveryId);

    /**
     * Trae las ofertas enviadas en un rango (tasa de aceptación, indicador I5).
     * <p>
     * EN: Gets the offers sent in a range (acceptance rate, indicator I5).
     *
     * @param from inicio del rango en minutos de reloj / range start in clock minutes
     * @param to fin del rango en minutos de reloj / range end in clock minutes
     * @return las ofertas / the offers
     */
    List<Offer> findBySentBetween(Double from, Double to);
}
