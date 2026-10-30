package co.serviasigna.persistence;

import co.serviasigna.domain.Address;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acceso a la tabla DIRECCION: direcciones guardadas de cada cliente.
 * <p>
 * Spring Data JPA escribe la implementación a partir del nombre de cada método: no hay que escribir SQL.
 * <p>
 * EN: Access to the DIRECCION table: saved addresses of each customer.
 * <p>
 * Spring Data JPA writes the implementation from each method name: no SQL needs to be written.
 */
public interface AddressRepository extends JpaRepository<Address, Long> {
    /**
     * Busca las direcciones activas de un cliente.
     * <p>
     * EN: Finds the active addresses of a customer.
     *
     * @param customerId id del cliente / customer id
     * @return las direcciones activas / the active addresses
     */
    List<Address> findByCustomerIdAndActiveTrue(Long customerId);
}
