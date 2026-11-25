package dev.harbour.bank.customers.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class CustomerTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);

    @Test
    void registersActiveCustomerWithNormalizedEmail() {
        Customer customer = Customer.register("  Ada Lovelace ", "  Ada@Example.COM ", CLOCK);

        assertThat(customer.id()).isNotNull();
        assertThat(customer.fullName()).isEqualTo("Ada Lovelace");
        assertThat(customer.email()).isEqualTo("ada@example.com");
        assertThat(customer.status()).isEqualTo(CustomerStatus.ACTIVE);
        assertThat(customer.createdAt()).isEqualTo(Instant.parse("2026-01-01T00:00:00Z"));
    }

    @Test
    void canBeSuspendedAndReinstated() {
        Customer customer = Customer.register("Ada Lovelace", "ada@example.com", CLOCK);

        customer.suspend();
        assertThat(customer.isActive()).isFalse();

        customer.reinstate();
        assertThat(customer.isActive()).isTrue();
    }
}
