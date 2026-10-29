package co.serviasigna.persistence;

import co.serviasigna.domain.Courier;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acceso a la tabla DOMICILIARIO: datos y estado operativo de cada domiciliario.
 * <p>
 * Spring Data JPA escribe la implementación a partir del nombre de cada método: no hay que escribir SQL.
 * <p>
 * EN: Access to the DOMICILIARIO table: data and operating status of each courier.
 * <p>
 * Spring Data JPA writes the implementation from each method name: no SQL needs to be written.
 */
public interface CourierRepository extends JpaRepository<Courier, Long> {}
