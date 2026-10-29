package co.serviasigna.persistence;

import co.serviasigna.domain.Shift;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acceso a la tabla TURNO: turnos de trabajo de los domiciliarios.
 * <p>
 * Spring Data JPA escribe la implementación a partir del nombre de cada método: no hay que escribir SQL.
 * <p>
 * EN: Access to the TURNO table: work shifts of the couriers.
 * <p>
 * Spring Data JPA writes the implementation from each method name: no SQL needs to be written.
 */
public interface ShiftRepository extends JpaRepository<Shift, Long> {
    /**
     * Busca los turnos que empezaron en un rango (horas de operación).
     * <p>
     * EN: Finds the shifts that started in a range (operating hours).
     *
     * @param from inicio del rango en minutos de reloj / range start in clock minutes
     * @param to fin del rango en minutos de reloj / range end in clock minutes
     * @return los turnos / the shifts
     */
    List<Shift> findByStartBetween(Double from, Double to);

    /**
     * Busca los turnos en un estado: "ABIERTO" o "CERRADO" (así se guardan en Oracle).
     * <p>
     * EN: Finds the shifts in a status: "ABIERTO" (open) or "CERRADO" (closed), as stored in Oracle.
     *
     * @param status estado buscado / wanted status
     * @return los turnos / the shifts
     */
    List<Shift> findByStatus(String status);
}
