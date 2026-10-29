package co.serviasigna.persistence;

/**
 * Explica cómo funcionan los repositorios de este paquete.
 * <p>
 * Son interfaces de Spring Data JPA: Spring escribe el SQL a partir del nombre del método. Por ejemplo,
 * {@code findByStatusIn(...)} equivale a {@code SELECT ... WHERE ESTADO IN (...)}. No hay que escribir su
 * implementación.
 * <p>
 * EN: Explains how the repositories of this package work.
 * <p>
 * They are Spring Data JPA interfaces: Spring writes the SQL from the method name. For example,
 * {@code findByStatusIn(...)} is the same as {@code SELECT ... WHERE ESTADO IN (...)}. Their implementation does
 * not need to be written.
 */
public final class Repositories { private Repositories() {} }
