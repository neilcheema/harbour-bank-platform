package dev.harbour.bank.customers.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.harbour.bank.customers.api.CustomerView;
import dev.harbour.bank.customers.domain.Customer;
import dev.harbour.bank.customers.infra.InMemoryCustomerRepository;
import dev.harbour.bank.shared.error.ConflictException;
import dev.harbour.bank.shared.error.NotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Plain unit tests: no Spring, no mocks. The in-memory repository is simple enough to use directly. */
class CustomerServiceTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
    private final CustomerService service = new CustomerService(new InMemoryCustomerRepository(), clock);

    @Test
    void registersAndRetrievesCustomer() {
        Customer registered = service.register("Ada Lovelace", "Ada@Example.com");

        assertThat(registered.email()).isEqualTo("ada@example.com");
        assertThat(service.get(registered.id())).isSameAs(registered);
        assertThat(service.list()).containsExactly(registered);
    }

    @Test
    void rejectsDuplicateEmailIgnoringCase() {
        service.register("Ada Lovelace", "ada@example.com");

        assertThatThrownBy(() -> service.register("Another Ada", "ADA@example.com"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("email");
    }

    @Test
    void getUnknownCustomerThrowsNotFound() {
        UUID unknown = UUID.randomUUID();

        assertThatThrownBy(() -> service.get(unknown))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining(unknown.toString());
    }

    @Test
    void directoryExposesOnlyIdAndActiveFlag() {
        Customer customer = service.register("Ada Lovelace", "ada@example.com");

        assertThat(service.find(customer.id())).contains(new CustomerView(customer.id(), true));

        customer.suspend();
        assertThat(service.find(customer.id())).contains(new CustomerView(customer.id(), false));
        assertThat(service.find(UUID.randomUUID())).isEmpty();
    }
}
