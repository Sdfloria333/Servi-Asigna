package co.serviasigna.persistence;

import co.serviasigna.domain.Setting;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acceso a la tabla PARAMETRO: parámetros de operación que cambia el administrador (la llave es la clave del
 * parámetro).
 * <p>
 * Spring Data JPA escribe la implementación a partir del nombre de cada método: no hay que escribir SQL.
 * <p>
 * EN: Access to the PARAMETRO table: operating settings changed by the administrator (the key is the setting name).
 * <p>
 * Spring Data JPA writes the implementation from each method name: no SQL needs to be written.
 */
public interface SettingRepository extends JpaRepository<Setting, String> {}
