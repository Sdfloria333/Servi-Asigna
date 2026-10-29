package co.serviasigna.config;

import org.springframework.boot.autoconfigure.orm.jpa.EntityManagerFactoryDependsOnPostProcessor;
import org.springframework.stereotype.Component;

/**
 * Obliga a que el migrador de esquema termine antes de que se cree JPA, igual que hace Spring Boot con Flyway.
 * <p>
 * Así Hibernate nunca encuentra una tabla que todavía no existe.
 * <p>
 * EN: Forces the schema migrator to finish before JPA is created, the same way Spring Boot does with Flyway.
 * <p>
 * This way Hibernate never finds a table that does not exist yet.
 */
@Component
public class MigrationBeforeJpa extends EntityManagerFactoryDependsOnPostProcessor {
    /**
     * Declara que la fábrica de JPA depende del bean "schemaMigrator".
     * <p>
     * EN: Declares that the JPA factory depends on the "schemaMigrator" bean.
     */
    public MigrationBeforeJpa() { super("schemaMigrator"); }
}
