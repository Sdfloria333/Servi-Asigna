package co.serviasigna.persistence;

import co.serviasigna.domain.Customer;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acceso a la tabla CLIENTE: clientes frecuentes que se buscan por teléfono o nombre.
 * <p>
 * Spring Data JPA escribe la implementación a partir del nombre de cada método: no hay que escribir SQL.
 * <p>
 * EN: Access to the CLIENTE table: frequent customers searched by phone or name.
 * <p>
 * Spring Data JPA writes the implementation from each method name: no SQL needs to be written.
 */
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    /**
     * Busca un cliente por su teléfono (el teléfono es único).
     * <p>
     * EN: Finds a customer by phone (the phone is unique).
     *
     * @param phone teléfono del cliente / customer phone
     * @return el cliente, si existe / the customer, if it exists
     */
    Optional<Customer> findByPhone(String phone);

    /**
     * Busca hasta 30 clientes activos cuyo nombre contenga el texto, sin importar mayúsculas.
     * <p>
     * EN: Finds up to 30 active customers whose name contains the text, ignoring case.
     *
     * @param name texto a buscar / text to search
     * @return los clientes ordenados por nombre / the customers sorted by name
     */
    List<Customer> findTop30ByActiveTrueAndNameContainingIgnoreCaseOrderByNameAsc(String name);

    /**
     * Busca hasta 30 clientes activos cuyo teléfono contenga los dígitos.
     * <p>
     * EN: Finds up to 30 active customers whose phone contains the digits.
     *
     * @param phone dígitos a buscar / digits to search
     * @return los clientes ordenados por nombre / the customers sorted by name
     */
    List<Customer> findTop30ByActiveTrueAndPhoneContainingOrderByNameAsc(String phone);
}
