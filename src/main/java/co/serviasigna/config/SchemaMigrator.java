package co.serviasigna.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

/**
 * Aplica los scripts versionados de la base de datos: el esquema vive en el código y no se crea a mano
 * (mantenibilidad).
 * <p>
 * Al arrancar, antes de que JPA use la base, ejecuta en orden los archivos
 * {@code classpath:db/migracion/V{n}__{descripcion}.sql} que aún no están en la tabla ESQUEMA_VERSION. Funciona
 * como Flyway pero sin depender de él (la edición gratuita de Flyway ya no acepta Oracle 18c XE). Cada script
 * guarda su suma SHA-256: si alguien edita un script ya aplicado, la aplicación no arranca y avisa; los cambios
 * nuevos van en un V{n+1}.
 * <p>
 * Reglas para escribir scripts: una sentencia por bloque terminada en ";" al final de la línea, comentarios con
 * "--" y sin bloques PL/SQL (BEGIN ... END;).
 * <p>
 * EN: Applies the versioned database scripts: the schema lives in the code and is not created by hand
 * (maintainability).
 * <p>
 * At startup, before JPA uses the database, it runs in order the
 * {@code classpath:db/migracion/V{n}__{description}.sql} files that are not yet in the ESQUEMA_VERSION table. It
 * works like Flyway without depending on it (the free edition of Flyway no longer supports Oracle 18c XE). Each
 * script stores its SHA-256 checksum: if someone edits a script that was already applied, the application does not
 * start and says so; new changes go in a V{n+1}.
 * <p>
 * Rules for writing scripts: one statement per block ending with ";" at the end of the line, comments with "--" and
 * no PL/SQL blocks (BEGIN ... END;).
 */
@Component("schemaMigrator")
public class SchemaMigrator {
    /** Registro de la aplicación. EN: Application logger. */
    private static final Logger log = LoggerFactory.getLogger(SchemaMigrator.class);
    /** Forma del nombre de un script: V{n}__{descripcion}.sql. EN: Shape of a script name: V{n}__{description}.sql. */
    private static final Pattern NAME_PATTERN = Pattern.compile("V(\\d+)__(.+)\\.sql");

    /**
     * Lee los scripts del classpath, revisa que no haya versiones repetidas y aplica los pendientes.
     * <p>
     * EN: Reads the scripts from the classpath, checks that no version is repeated and applies the pending ones.
     *
     * @param data conexión a la base de datos / database connection
     * @throws Exception si un script falla o cambió después de aplicarse / if a script fails or changed after being
     *     applied
     */
    public SchemaMigrator(DataSource data) throws Exception {
        Map<Integer, Script> scripts = new TreeMap<>();
        for (Resource r : new PathMatchingResourcePatternResolver().getResources("classpath*:db/migracion/V*.sql")) {
            Matcher m = NAME_PATTERN.matcher(r.getFilename() == null ? "" : r.getFilename());
            if (!m.matches()) continue;
            try (InputStream in = r.getInputStream()) {
                Script s = new Script(Integer.parseInt(m.group(1)), m.group(2).replace('_', ' '), r.getFilename(),
                        new String(in.readAllBytes(), StandardCharsets.UTF_8));
                if (scripts.put(s.version(), s) != null) throw new IllegalStateException("Hay dos scripts con la versión V" + s.version());
            }
        }
        try (Connection c = data.getConnection()) {
            List<String> applied = migrate(c, new ArrayList<>(scripts.values()));
            if (applied.isEmpty()) log.info("Base de datos al día ({} scripts)", scripts.size());
            else log.info("Scripts aplicados: {}", applied);
        }
    }

    /**
     * Un script de migración leído del classpath.
     * <p>
     * EN: A migration script read from the classpath.
     *
     * @param version número de versión (la n de V{n}) / version number (the n in V{n})
     * @param description descripción tomada del nombre del archivo / description taken from the file name
     * @param file nombre del archivo / file name
     * @param content texto SQL del script / SQL text of the script
     */
    public record Script(int version, String description, String file, String content) {
        /**
         * Calcula la suma SHA-256 del contenido, sin importar si las líneas terminan en CRLF o LF.
         * <p>
         * EN: Computes the SHA-256 checksum of the content, no matter if lines end in CRLF or LF.
         *
         * @return la suma en hexadecimal / the checksum in hexadecimal
         */
        String sum() {
            try {
                String normalized = content.replace("\r\n", "\n");
                return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(normalized.getBytes(StandardCharsets.UTF_8)));
            } catch (Exception e) { throw new IllegalStateException(e); }
        }
    }

    /**
     * Aplica los scripts pendientes y anota cada uno en ESQUEMA_VERSION. Es público para poder probarlo sin Spring.
     * <p>
     * EN: Applies the pending scripts and records each one in ESQUEMA_VERSION. It is public so it can be tested without
     * Spring.
     *
     * @param c conexión abierta / open connection
     * @param scripts scripts ordenados por versión / scripts sorted by version
     * @return los nombres de los scripts que se ejecutaron / the names of the scripts that were run
     * @throws SQLException si falla la lectura de ESQUEMA_VERSION / if reading ESQUEMA_VERSION fails
     * @throws IllegalStateException si un script falla o cambió después de aplicarse / if a script fails or changed
     *     after being applied
     */
    public static List<String> migrate(Connection c, List<Script> scripts) throws SQLException {
        createVersionTable(c);
        Map<Integer, String> appliedSums = new LinkedHashMap<>();
        try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery("SELECT VERSION, SUMA FROM ESQUEMA_VERSION")) {
            while (rs.next()) appliedSums.put(rs.getInt(1), rs.getString(2));
        }
        List<String> applied = new ArrayList<>();
        for (Script s : scripts) {
            String sum = appliedSums.get(s.version());
            if (sum != null) {
                if (!sum.equals(s.sum()))
                    throw new IllegalStateException("El script " + s.file() + " cambió después de aplicarse. No edites scripts ya aplicados: "
                            + "crea uno nuevo (V" + (s.version() + 1) + "__...) o, en desarrollo, borra el usuario de Oracle y vuelve a crearlo.");
                continue;
            }
            List<String> statements = statements(s.content());
            int n = 0;
            try (Statement st = c.createStatement()) {
                for (String sql : statements) { n++; st.execute(sql); }
            } catch (SQLException e) {
                throw new IllegalStateException("Falló " + s.file() + " en la sentencia " + n + " de " + statements.size() + ": " + e.getMessage()
                        + "\n" + statements.get(n - 1) + "\nOracle no deshace el DDL ya ejecutado: borra el usuario (DROP USER DESPACHO CASCADE), "
                        + "créalo de nuevo con db/oracle/00_crear_usuario.sql y vuelve a arrancar.", e);
            }
            try (PreparedStatement ps = c.prepareStatement("INSERT INTO ESQUEMA_VERSION (VERSION, DESCRIPCION, ARCHIVO, SUMA, APLICADO) VALUES (?, ?, ?, ?, ?)")) {
                ps.setInt(1, s.version());
                ps.setString(2, s.description());
                ps.setString(3, s.file());
                ps.setString(4, s.sum());
                ps.setTimestamp(5, new Timestamp(System.currentTimeMillis()));
                ps.executeUpdate();
            }
            if (!c.getAutoCommit()) c.commit();
            applied.add(s.file());
        }
        return applied;
    }

    /**
     * Crea la tabla ESQUEMA_VERSION si todavía no existe.
     * <p>
     * EN: Creates the ESQUEMA_VERSION table if it does not exist yet.
     *
     * @param c conexión abierta / open connection
     * @throws SQLException si no se puede crear la tabla / if the table cannot be created
     */
    private static void createVersionTable(Connection c) throws SQLException {
        DatabaseMetaData md = c.getMetaData();
        String schema = c.getSchema();
        try (ResultSet rs = md.getTables(null, schema, "ESQUEMA_VERSION", new String[] { "TABLE" })) {
            if (rs.next()) return;
        }
        try (Statement st = c.createStatement()) {
            st.execute("CREATE TABLE ESQUEMA_VERSION (VERSION NUMBER(5) PRIMARY KEY, DESCRIPCION VARCHAR2(100 CHAR) NOT NULL, "
                    + "ARCHIVO VARCHAR2(120 CHAR) NOT NULL, SUMA VARCHAR2(64 CHAR) NOT NULL, APLICADO TIMESTAMP NOT NULL)");
        }
    }

    /**
     * Parte el archivo en sentencias: quita los comentarios "--" y corta en cada ";" de fin de línea.
     * <p>
     * EN: Splits the file into statements: removes "--" comments and cuts at every ";" at the end of a line.
     *
     * @param content texto del script / script text
     * @return las sentencias, sin el ";" final / the statements, without the final ";"
     */
    static List<String> statements(String content) {
        List<String> list = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String line : content.replace("\r\n", "\n").split("\n")) {
            String l = withoutComment(line).stripTrailing();
            if (l.isBlank()) continue;
            if (l.endsWith(";")) {
                current.append(l, 0, l.length() - 1);
                if (!current.toString().isBlank()) list.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(l).append('\n');
            }
        }
        if (!current.toString().isBlank()) list.add(current.toString().trim());
        return list;
    }

    /**
     * Quita lo que sigue a "--" cuando no está dentro de comillas simples.
     * <p>
     * EN: Removes what follows "--" when it is not inside single quotes.
     *
     * @param line línea del script / script line
     * @return la línea sin comentario / the line without comment
     */
    private static String withoutComment(String line) {
        boolean quotes = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '\'') quotes = !quotes;
            else if (!quotes && ch == '-' && i + 1 < line.length() && line.charAt(i + 1) == '-') return line.substring(0, i);
        }
        return line;
    }

    /**
     * Lee todo un flujo como texto UTF-8.
     * <p>
     * EN: Reads a whole stream as UTF-8 text.
     *
     * @param in flujo de entrada / input stream
     * @return el texto leído / the text read
     * @throws IOException si no se puede leer / if it cannot be read
     */
    static String read(InputStream in) throws IOException { return new String(in.readAllBytes(), StandardCharsets.UTF_8); }
}
