package dev.harbour.bank.support;

import dev.harbour.bank.accounts.infra.JpaAccountRepository;
import dev.harbour.bank.accounts.infra.SequenceAccountNumberGenerator;
import dev.harbour.bank.customers.infra.JpaCustomerRepository;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

/**
 * Meta-annotation for repository tests: a JPA-only Spring context (entities, repositories, Flyway)
 * running against a real PostgreSQL. Test classes using it must extend {@link PostgresTestSupport},
 * which supplies the database. Each test runs in a transaction that is rolled back afterwards.
 *
 * <p>All repository tests share this exact configuration, so Spring builds the context once and
 * reuses it.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@DataJpaTest
// By default @DataJpaTest swaps in an in-memory database. We want the real PostgreSQL instead.
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
// @DataJpaTest does not scan @Repository/@Component classes, so list the ones under test.
@Import({JpaCustomerRepository.class, JpaAccountRepository.class, SequenceAccountNumberGenerator.class})
public @interface RepositoryTest {}
