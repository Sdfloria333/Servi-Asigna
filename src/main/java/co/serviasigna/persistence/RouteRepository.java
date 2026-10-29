package co.serviasigna.persistence;

import co.serviasigna.domain.Route;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acceso a la tabla RUTA: la ruta activa de cada domiciliario.
 * <p>
 * Spring Data JPA escribe la implementación a partir del nombre de cada método: no hay que escribir SQL.
 * <p>
 * EN: Access to the RUTA table: the active route of each courier.
 * <p>
 * Spring Data JPA writes the implementation from each method name: no SQL needs to be written.
 */
public interface RouteRepository extends JpaRepository<Route, Long> {}
