package co.serviasigna.persistence;

import co.serviasigna.domain.Rate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acceso a la tabla TARIFA: tarifas con las que se calcula el valor sugerido.
 * <p>
 * Spring Data JPA escribe la implementación a partir del nombre de cada método: no hay que escribir SQL.
 * <p>
 * EN: Access to the TARIFA table: rates used to compute the suggested value.
 * <p>
 * Spring Data JPA writes the implementation from each method name: no SQL needs to be written.
 */
public interface RateRepository extends JpaRepository<Rate, Long> {
    /**
     * Busca la tarifa activa más reciente.
     * <p>
     * EN: Finds the most recent active rate.
     *
     * @return la tarifa vigente, si hay / the current rate, if any
     */
    Optional<Rate> findFirstByActiveTrueOrderByIdDesc();
}
