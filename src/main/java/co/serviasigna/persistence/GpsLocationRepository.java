package co.serviasigna.persistence;

import co.serviasigna.domain.GpsLocation;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Acceso a la tabla UBICACION_GPS: historial de posiciones de los domiciliarios.
 * <p>
 * Spring Data JPA escribe la implementación a partir del nombre de cada método: no hay que escribir SQL.
 * <p>
 * EN: Access to the UBICACION_GPS table: position history of the couriers.
 * <p>
 * Spring Data JPA writes the implementation from each method name: no SQL needs to be written.
 */
public interface GpsLocationRepository extends JpaRepository<GpsLocation, Long> {
    /**
     * Trae las posiciones de un rango, agrupadas por domiciliario y en orden de captura (km recorridos).
     * <p>
     * EN: Gets the positions in a range, grouped by courier and in capture order (distance travelled).
     *
     * @param from inicio del rango en minutos de reloj / range start in clock minutes
     * @param to fin del rango en minutos de reloj / range end in clock minutes
     * @return las posiciones / the positions
     */
    List<GpsLocation> findByCapturedAtBetweenOrderByCourierIdAscCapturedAtAsc(Double from, Double to);

    /**
     * Borra las posiciones anteriores a una fecha: depuración automática del historial (RNF-10).
     * <p>
     * EN: Deletes the positions older than a date: automatic history cleanup (RNF-10).
     *
     * @param before fecha límite en minutos de reloj / cutoff date in clock minutes
     * @return cuántas filas se borraron / how many rows were deleted
     */
    @Transactional
    long deleteByCapturedAtLessThan(Double before);
}
