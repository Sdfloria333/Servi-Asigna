package co.serviasigna.users;

import co.serviasigna.domain.AccountStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acceso a la tabla USUARIO: cuentas de administradores, despachadores y domiciliarios.
 * <p>
 * Spring Data JPA escribe la implementación a partir del nombre de cada método: no hay que escribir SQL.
 * <p>
 * EN: Access to the USUARIO table: accounts of administrators, dispatchers and couriers.
 * <p>
 * Spring Data JPA writes the implementation from each method name: no SQL needs to be written.
 */
public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
    /**
     * Busca una cuenta por su nombre de usuario.
     * <p>
     * EN: Finds an account by its username.
     *
     * @param username nombre de usuario / username
     * @return la cuenta, si existe / the account, if it exists
     */
    Optional<UserAccount> findByUsername(String username);

    /**
     * Indica si ya existe una cuenta con ese nombre de usuario.
     * <p>
     * EN: Tells whether an account with that username already exists.
     *
     * @param username nombre de usuario / username
     * @return true si ya existe / true if it exists
     */
    boolean existsByUsername(String username);

    /**
     * Indica si hay al menos una cuenta con ese rol (para crear las cuentas iniciales).
     * <p>
     * EN: Tells whether there is at least one account with that role (to create the initial accounts).
     *
     * @param role rol buscado / wanted role
     * @return true si hay alguna / true if there is any
     */
    boolean existsByRole(Role role);

    /**
     * Cuenta las cuentas que están en un estado (por ejemplo, las pendientes de aprobación).
     * <p>
     * EN: Counts the accounts in a status (for example, the ones waiting for approval).
     *
     * @param status estado de la cuenta / account status
     * @return cuántas hay / how many there are
     */
    long countByAccountStatus(AccountStatus status);

    /**
     * Trae todas las cuentas en orden de creación.
     * <p>
     * EN: Gets every account in creation order.
     *
     * @return las cuentas / the accounts
     */
    List<UserAccount> findAllByOrderByIdAsc();

    /**
     * Trae las cuentas que tienen un rol.
     * <p>
     * EN: Gets the accounts that have a role.
     *
     * @param role rol buscado / wanted role
     * @return las cuentas / the accounts
     */
    List<UserAccount> findByRole(Role role);
}
