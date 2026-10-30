package co.serviasigna.domain;

import jakarta.persistence.*;

/**
 * Parámetro operativo que el administrador cambia desde la pantalla (tabla PARAMETRO, RF-M2-08).
 * <p>
 * La llave (por ejemplo {@code motor.modo}) y el valor se guardan como texto; la clase Settings los valida y
 * convierte.
 * <p>
 * EN: Operating setting the administrator changes from the screen (PARAMETRO table, RF-M2-08).
 * <p>
 * The key (for example {@code motor.modo}) and the value are stored as text; the Settings class validates and
 * converts them.
 */
@Entity
@Table(name = "PARAMETRO")
public class Setting {
    /** La llave del parámetro (columna CLAVE). EN: The setting key (CLAVE column). */
    @Id @Column(name = "CLAVE", length = 60) private String key;
    /** El valor actual como texto. EN: The current value as text. */
    @Column(name = "VALOR", nullable = false, length = 200) private String value;
    /** La explicación que ve el administrador. EN: The explanation shown to the administrator. */
    @Column(name = "DESCRIPCION", length = 300) private String description;

    /**
     * Constructor vacío que exige JPA para leer la fila de la base de datos.
     * <p>
     * EN: Empty constructor required by JPA to read the row from the database.
     */
    protected Setting() {}

    /**
     * Crea un parámetro.
     * <p>
     * EN: Creates a setting.
     *
     * @param key llave del parámetro / setting key
     * @param value valor inicial / initial value
     * @param description explicación que ve el administrador / explanation shown to the administrator
     */
    public Setting(String key, String value, String description) { this.key = key; this.value = value; this.description = description; }

    /**
     * Cambia el valor actual como texto.
     * <p>
     * EN: Changes the current value as text.
     *
     * @param v el nuevo valor / the new value
     */
    public void setValue(String v) { value = v; }

    /**
     * Devuelve la llave del parámetro (columna CLAVE).
     * <p>
     * EN: Returns the setting key (CLAVE column).
     *
     * @return la llave del parámetro (columna CLAVE) / the setting key (CLAVE column)
     */
    public String getKey() { return key; }

    /**
     * Devuelve el valor actual como texto.
     * <p>
     * EN: Returns the current value as text.
     *
     * @return el valor actual como texto / the current value as text
     */
    public String getValue() { return value; }

    /**
     * Devuelve la explicación que ve el administrador.
     * <p>
     * EN: Returns the explanation shown to the administrator.
     *
     * @return la explicación que ve el administrador / the explanation shown to the administrator
     */
    public String getDescription() { return description; }
}
