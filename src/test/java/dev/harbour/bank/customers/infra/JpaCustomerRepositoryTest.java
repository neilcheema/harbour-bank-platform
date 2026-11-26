package dev.harbour.bank.customers.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.harbour.bank.customers.domain.Customer;
import dev.harbour.bank.customers.domain.CustomerRepository;
import dev.harbour.bank.customers.domain.CustomerStatus;
import dev.harbour.bank.support.PostgresTestSupport;
import dev.harbour.bank.support.RepositoryTest;
import jakarta.persistence.PersistenceException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Talks to a real PostgreSQL. Pattern used throughout: save, then {@code flush()} to force the SQL to
 * run now, then {@code clear()} to empty Hibernate's cache, so the next read really goes to the
 * database instead of returning the object we just saved.
 */
@RepositoryTest
class JpaCustomerRepositoryTest extends PostgresTestSupport {

    private static Clock clockAt(String instant) {
        return Clock.fixed(Instant.parse(instant), ZoneOffset.UTC);
    }

    private static final Clock CLOCK = clockAt("2026-01-01T00:00:00Z");

    @Autowired
    CustomerRepository repository;

    @Autowired
    TestEntityManager em;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void savesAndReadsBackEveryField() {
        Customer saved = repository.save(Customer.register("Ada Lovelace", "ada@example.com", CLOCK));
        em.flush();
        em.clear();

        Customer found = repository.findById(saved.id()).orElseThrow();

        assertThat(found).isNotSameAs(saved); // a fresh object loaded from the database
        assertThat(found.id()).isEqualTo(saved.id());
        assertThat(found.fullName()).isEqualTo("Ada Lovelace");
        assertThat(found.email()).isEqualTo("ada@example.com");
        assertThat(found.status()).isEqualTo(CustomerStatus.ACTIVE);
        assertThat(found.createdAt()).isEqualTo(Instant.parse("2026-01-01T00:00:00Z"));
    }

    @Test
    void findByEmailMatchesExactNormalizedEmail() {
        repository.save(Customer.register("Ada Lovelace", "ada@example.com", CLOCK));
        em.flush();
        em.clear();

        assertThat(repository.findByEmail("ada@example.com")).isPresent();
        assertThat(repository.findByEmail("nobody@example.com")).isEmpty();
    }

    @Test
    void unknownIdIsEmpty() {
        assertThat(repository.findById(UUID.randomUUID())).isEmpty();
    }

    @Test
    void databaseRejectsDuplicateEmail() {
        repository.save(Customer.register("Ada Lovelace", "ada@example.com", CLOCK));
        repository.save(Customer.register("Another Ada", "ada@example.com", CLOCK));

        // The INSERTs run at flush time; the second one violates the UNIQUE constraint.
        assertThatThrownBy(() -> em.flush())
                .isInstanceOf(PersistenceException.class)
                .rootCause()
                .hasMessageContaining("uq_customers_email");
    }

    @Test
    void listsOldestFirst() {
        Customer newer = repository.save(Customer.register("Newer", "newer@example.com", clockAt("2026-03-01T00:00:00Z")));
        Customer older = repository.save(Customer.register("Older", "older@example.com", clockAt("2026-02-01T00:00:00Z")));
        em.flush();
        em.clear();

        // The database is shared, so other rows may exist; we only assert the relative order of ours.
        assertThat(repository.findAll()).extracting(Customer::id).containsSubsequence(older.id(), newer.id());
    }

    @Test
    void statusChangeIsPersistedAndBumpsTheVersion() {
        Customer saved = repository.save(Customer.register("Ada Lovelace", "ada@example.com", CLOCK));
        em.flush();
        assertThat(versionOf(saved.id())).isZero();
        em.clear();

        Customer loaded = repository.findById(saved.id()).orElseThrow();
        loaded.suspend();
        repository.save(loaded);
        em.flush();
        em.clear();

        assertThat(repository.findById(saved.id()).orElseThrow().status()).isEqualTo(CustomerStatus.SUSPENDED);
        assertThat(versionOf(saved.id())).isEqualTo(1L);
    }

    private long versionOf(UUID id) {
        Long version = jdbc.queryForObject("select version from customers where id = ?", Long.class, id);
        return version == null ? -1 : version;
    }
}
