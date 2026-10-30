package co.serviasigna.domain;

/**
 * Enumeración que se guarda en la base de datos con un código en español.
 * <p>
 * Los valores de las enumeraciones están en inglés, pero las tablas de Oracle conservan los códigos originales en español
 * (por ejemplo {@code EN_COLA}) porque así lo exigen sus restricciones CHECK, los datos de prueba y las consultas SQL.
 * Cada valor sabe cuál es su código y {@link DbCodeConverters} hace la traducción al leer y al guardar.
 * <p>
 * EN: Enumeration stored in the database with a Spanish code.
 * Enum values are in English, but the Oracle tables keep the original Spanish codes (for example {@code EN_COLA})
 * because their CHECK constraints, the demo data and the SQL queries require them. Each value knows its code and
 * {@link DbCodeConverters} translates it when reading and writing.
 */
public interface DbCoded {

    /**
     * Devuelve el código con el que este valor se guarda en la base de datos.
     * <p>
     * EN: Returns the code this value is stored with in the database.
     *
     * @return el código en español de la base de datos / the Spanish database code
     */
    String dbCode();

    /**
     * Busca el valor de una enumeración a partir de su código de base de datos.
     * También acepta el nombre del valor en inglés, para que la API pueda recibir cualquiera de los dos.
     * <p>
     * EN: Finds an enum value from its database code.
     * It also accepts the English value name, so the API can receive either of them.
     *
     * @param type la clase de la enumeración / the enum class
     * @param code el código en español o el nombre en inglés / the Spanish code or the English name
     * @param <E>  el tipo de la enumeración / the enum type
     * @return el valor encontrado / the matching value
     * @throws IllegalArgumentException si ningún valor tiene ese código ni ese nombre /
     *                                  if no value has that code or that name
     */
    static <E extends Enum<E> & DbCoded> E fromDbCode(Class<E> type, String code) {
        String wanted = code == null ? "" : code.trim().toUpperCase();
        for (E value : type.getEnumConstants()) {
            if (value.dbCode().equals(wanted) || value.name().equals(wanted)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Valor desconocido para " + type.getSimpleName() + ": " + code);
    }
}
