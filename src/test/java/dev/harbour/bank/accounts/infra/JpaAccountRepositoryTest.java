package dev.harbour.bank.accounts.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.harbour.bank.accounts.application.AccountNumberGenerator;
import dev.harbour.bank.accounts.domain.Account;
import dev.harbour.bank.accounts.domain.AccountRepository;
import dev.harbour.bank.accounts.domain.AccountStatus;
import dev.harbour.bank.accounts.domain.AccountType;
import dev.harbour.bank.customers.domain.Customer;
import dev.harbour.bank.support.PostgresTestSupport;
import dev.harbour.bank.support.RepositoryTest;
import jakarta.persistence.PersistenceException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Currency;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

@RepositoryTest
class JpaAccountRepositoryTest extends PostgresTestSupport {

    private static Clock clockAt(String instant) {
        return Clock.fixed(Instant.parse(instant), ZoneOffset.UTC);
    }

    private static final Clock CLOCK = clockAt("2026-01-01T00:00:00Z");
    private static final Currency CAD = Currency.getInstance("CAD");

    @Autowired
    AccountRepository repository;

    @Autowired
    AccountNumberGenerator numbers;

    @Autowired
    TestEntityManager em;

    @Autowired
    JdbcTemplate jdbc;

    /** Accounts reference a customer row (foreign key), so tests that save accounts need one first. */
    private UUID persistedCustomer(String email) {
        Customer customer = em.persist(Customer.register("Test Customer", email, CLOCK));
        em.flush();
        return customer.id();
    }

    @Test
    void savesAndReadsBackEveryField() {
        UUID customerId = persistedCustomer("a@example.com");
        Account saved = repository.save(Account.open(customerId, "HB-1111111111", AccountType.TFSA, CAD, CLOCK));
        em.flush();
        em.clear();

        Account found = repository.findById(saved.id()).orElseThrow();

        assertThat(found).isNotSameAs(saved);
        assertThat(found.customerId()).isEqualTo(customerId);
        assertThat(found.accountNumber()).isEqualTo("HB-1111111111");
        assertThat(found.type()).isEqualTo(AccountType.TFSA);
        assertThat(found.currency()).isEqualTo(CAD);
        assertThat(found.status()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(found.createdAt()).isEqualTo(Instant.parse("2026-01-01T00:00:00Z"));
    }

    @Test
    void statusChangeIsPersisted() {
        UUID customerId = persistedCustomer("b@example.com");
        Account saved = repository.save(Account.open(customerId, "HB-2222222222", AccountType.CHEQUING, CAD, CLOCK));
        em.flush();
        em.clear();

        Account loaded = repository.findById(saved.id()).orElseThrow();
        loaded.changeStatus(AccountStatus.FROZEN);
        repository.save(loaded);
        em.flush();
        em.clear();

        assertThat(repository.findById(saved.id()).orElseThrow().status()).isEqualTo(AccountStatus.FROZEN);
    }

    @Test
    void findsOnlyTheCustomersAccountsOldestFirst() {
        UUID ada = persistedCustomer("ada@example.com");
        UUID alan = persistedCustomer("alan@example.com");
        Account newer = repository.save(Account.open(
                ada, "HB-3000000002", AccountType.SAVINGS, CAD, clockAt("2026-03-01T00:00:00Z")));
        Account older = repository.save(Account.open(
                ada, "HB-3000000001", AccountType.CHEQUING, CAD, clockAt("2026-02-01T00:00:00Z")));
        Account alans = repository.save(Account.open(alan, "HB-3000000003", AccountType.CHEQUING, CAD, CLOCK));
        em.flush();
        em.clear();

        // Filtering by customer is exact: it contains Ada's two accounts, oldest first, and nothing else.
        assertThat(repository.findByCustomerId(ada)).extracting(Account::id).containsExactly(older.id(), newer.id());
        assertThat(repository.findByCustomerId(UUID.randomUUID())).isEmpty();
        // The database is shared, so findAll may hold other rows too; check that ours are all present.
        assertThat(repository.findAll())
                .extracting(Account::id)
                .contains(older.id(), newer.id(), alans.id());
    }

    @Test
    void databaseRejectsAccountForUnknownCustomer() {
        repository.save(Account.open(UUID.randomUUID(), "HB-4000000001", AccountType.CHEQUING, CAD, CLOCK));

        assertThatThrownBy(() -> em.flush())
                .isInstanceOf(PersistenceException.class)
                .rootCause()
                .hasMessageContaining("fk_accounts_customer");
    }

    @Test
    void databaseRejectsDuplicateAccountNumber() {
        UUID customerId = persistedCustomer("c@example.com");
        repository.save(Account.open(customerId, "HB-5000000001", AccountType.CHEQUING, CAD, CLOCK));
        repository.save(Account.open(customerId, "HB-5000000001", AccountType.SAVINGS, CAD, CLOCK));

        assertThatThrownBy(() -> em.flush())
                .isInstanceOf(PersistenceException.class)
                .rootCause()
                .hasMessageContaining("uq_accounts_account_number");
    }

    // Two separate tests on purpose: after a failed statement PostgreSQL aborts the whole transaction,
    // so a second failing statement in the same test would report "transaction is aborted" instead.

    @Test
    void databaseRejectsAnInvalidStatusEvenWhenJavaIsBypassed() {
        UUID customerId = persistedCustomer("d@example.com");
        Account saved = repository.save(Account.open(customerId, "HB-6000000001", AccountType.CHEQUING, CAD, CLOCK));
        em.flush();

        // Raw SQL skips every Java check; the CHECK constraint is the last line of defence.
        assertThatThrownBy(() -> jdbc.update("update accounts set status = 'BOGUS' where id = ?", saved.id()))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_accounts_status");
    }

    @Test
    void databaseRejectsALowercaseCurrencyCodeEvenWhenJavaIsBypassed() {
        UUID customerId = persistedCustomer("e@example.com");
        Account saved = repository.save(Account.open(customerId, "HB-6000000002", AccountType.CHEQUING, CAD, CLOCK));
        em.flush();

        assertThatThrownBy(() -> jdbc.update("update accounts set currency = 'cad' where id = ?", saved.id()))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_accounts_currency");
    }

    @Test
    void sequenceIssuesIncreasingAccountNumbersInTheExpectedFormat() {
        String first = numbers.next();
        String second = numbers.next();

        assertThat(first).matches("HB-\\d{10}");
        assertThat(second).matches("HB-\\d{10}");
        assertThat(Long.parseLong(second.substring(3))).isGreaterThan(Long.parseLong(first.substring(3)));
    }
}
