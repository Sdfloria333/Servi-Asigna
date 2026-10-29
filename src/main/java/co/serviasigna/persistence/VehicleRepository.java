package co.serviasigna.persistence;

import co.serviasigna.domain.Vehicle;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acceso a la tabla VEHICULO: vehículos asignados a los domiciliarios.
 * <p>
 * Spring Data JPA escribe la implementación a partir del nombre de cada método: no hay que escribir SQL.
 * <p>
 * EN: Access to the VEHICULO table: vehicles assigned to the couriers.
 * <p>
 * Spring Data JPA writes the implementation from each method name: no SQL needs to be written.
 */
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    /**
     * Trae los vehículos activos.
     * <p>
     * EN: Gets the active vehicles.
     *
     * @return los vehículos / the vehicles
     */
    List<Vehicle> findByActiveTrue();

    /**
     * Trae los vehículos activos de un domiciliario.
     * <p>
     * EN: Gets the active vehicles of a courier.
     *
     * @param courierId id del domiciliario / courier id
     * @return los vehículos / the vehicles
     */
    List<Vehicle> findByCourierIdAndActiveTrue(Long courierId);
}
