package co.serviasigna.domain;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Tamaño del paquete en unidades de carga (RN-04).
 * <p>
 * Cada valor guarda su código en español para la base de datos ({@link #dbCode()}).
 * <p>
 * EN: Package size in load units (RN-04).
 * Each value keeps its Spanish database code ({@link #dbCode()}).
 */
public enum PackageSize implements DbCoded {

    /** Pequeño: 1 unidad. EN: Small: 1 unit. */
    SMALL("PEQUENO", 1),

    /** Mediano: 2 unidades. EN: Medium: 2 units. */
    MEDIUM("MEDIANO", 2),

    /** Grande: 4 unidades. EN: Large: 4 units. */
    LARGE("GRANDE", 4);

    /** Código en español que se guarda en la base de datos. EN: Spanish code stored in the database. */
    private final String dbCode;
    /** Unidades de carga que ocupa. EN: Load units it takes. */
    private final int units;

    /**
     * Crea un valor con su código de base de datos.
     * <p>
     * EN: Creates a value with its database code.
     *
     * @param dbCode el código en español que se guarda en Oracle / the Spanish code stored in Oracle
     * @param units unidades de carga que ocupa / load units it takes
     */
    PackageSize(String dbCode, int units) {
        this.dbCode = dbCode;
        this.units = units;
    }

    /**
     * Devuelve el código con el que este valor se guarda en la base de datos.
     * <p>
     * EN: Returns the code this value is stored with in the database.
     *
     * @return el código en español / the Spanish code
     */
    @Override
    public String dbCode() {
        return dbCode;
    }

    /**
     * Lee el valor que llega en un JSON: acepta el nombre en inglés o el código en español.
     * <p>
     * EN: Reads the value received in a JSON body: accepts the English name or the Spanish code.
     *
     * @param value el texto recibido / the received text
     * @return el valor encontrado / the matching value
     */
    @JsonCreator
    public static PackageSize fromJson(String value) {
        return DbCoded.fromDbCode(PackageSize.class, value);
    }

    /**
     * Devuelve cuántas unidades de carga ocupa el paquete.
     * <p>
     * EN: Returns how many load units the package takes.
     *
     * @return las unidades / the units
     */
    public int units() {
        return units;
    }
}
