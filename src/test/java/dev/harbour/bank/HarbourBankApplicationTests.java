package dev.harbour.bank;

import static org.assertj.core.api.Assertions.assertThat;

import dev.harbour.bank.support.PostgresTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Smoke test: the application starts against a real PostgreSQL (Testcontainers), Flyway applies the
 * migrations, and Hibernate's schema validation accepts the mapping. Requires Docker.
 */
@SpringBootTest
class HarbourBankApplicationTests extends PostgresTestSupport {

    @Autowired
    ApplicationContext context;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void contextLoads() {
        assertThat(context.getId()).isNotBlank();
    }

    @Test
    void flywayAppliedTheFirstMigration() {
        Integer applied = jdbc.queryForObject(
                "select count(*) from flyway_schema_history where version = '1' and success", Integer.class);
        assertThat(applied).isEqualTo(1);
    }
}
