package dev.harbour.bank.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base class for every test that needs a real PostgreSQL.
 *
 * <p>One container is started the first time any such test runs and is shared by all of them for the
 * rest of the test run (Testcontainers' helper container removes it when the JVM exits). That is much
 * faster than one container per test class.
 *
 * <p>{@code @DynamicPropertySource} runs after the container is up and hands its connection details to
 * Spring, so the application's DataSource points at the container. It works in every kind of Spring
 * test, including {@code @DataJpaTest} slices.
 *
 * <p>Because the database is shared, a test must never assume the tables are empty: other tests may
 * have committed rows. Tests that run inside a rolled-back transaction leave nothing behind.
 */
public abstract class PostgresTestSupport {

    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
}
