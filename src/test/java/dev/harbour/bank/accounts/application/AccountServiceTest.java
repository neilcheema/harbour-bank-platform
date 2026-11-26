package dev.harbour.bank.accounts.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.harbour.bank.accounts.domain.Account;
import dev.harbour.bank.accounts.domain.AccountStatus;
import dev.harbour.bank.accounts.domain.AccountType;
import dev.harbour.bank.accounts.infra.InMemoryAccountRepository;
import dev.harbour.bank.customers.api.CustomerDirectory;
import dev.harbour.bank.customers.api.CustomerView;
import dev.harbour.bank.shared.error.BusinessRuleException;
import dev.harbour.bank.shared.error.NotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

/**
 * The accounts module is tested against a fake {@link CustomerDirectory} (a lambda), which shows
 * the benefit of depending on a small interface instead of the customers module's internals.
 */
class AccountServiceTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
    private final UUID activeCustomer = UUID.randomUUID();
    private final UUID suspendedCustomer = UUID.randomUUID();

    private final CustomerDirectory directory = id -> {
        if (id.equals(activeCustomer)) {
            return Optional.of(new CustomerView(id, true));
        }
        if (id.equals(suspendedCustomer)) {
            return Optional.of(new CustomerView(id, false));
        }
        return Optional.empty();
    };

    private final InMemoryAccountRepository repository = new InMemoryAccountRepository();
    // AccountNumberGenerator is a one-method interface, so a lambda is a complete fake.
    private final AtomicLong nextNumber = new AtomicLong(1_000_000_000L);
    private final AccountNumberGenerator numbers = () -> "HB-%010d".formatted(nextNumber.getAndIncrement());

    private final AccountService service = new AccountService(repository, directory, numbers, clock);

    @Test
    void opensAnActiveAccountForAnActiveCustomer() {
        Account account = service.open(activeCustomer, AccountType.SAVINGS, "CAD");

        assertThat(account.customerId()).isEqualTo(activeCustomer);
        assertThat(account.type()).isEqualTo(AccountType.SAVINGS);
        assertThat(account.currency().getCurrencyCode()).isEqualTo("CAD");
        assertThat(account.status()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(account.accountNumber()).matches("HB-\\d{10}");
        assertThat(service.get(account.id())).isSameAs(account);
    }

    @Test
    void rejectsUnknownCustomer() {
        UUID unknown = UUID.randomUUID();

        assertThatThrownBy(() -> service.open(unknown, AccountType.CHEQUING, "CAD"))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining(unknown.toString());
    }

    @Test
    void rejectsSuspendedCustomer() {
        assertThatThrownBy(() -> service.open(suspendedCustomer, AccountType.CHEQUING, "CAD"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("not active");
    }

    @Test
    void rejectsUnsupportedCurrency() {
        assertThatThrownBy(() -> service.open(activeCustomer, AccountType.CHEQUING, "JPY"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("JPY");
    }

    @Test
    void allowsAtMostThreeOpenAccountsPerCustomer() {
        Account first = service.open(activeCustomer, AccountType.CHEQUING, "CAD");
        service.open(activeCustomer, AccountType.SAVINGS, "CAD");
        service.open(activeCustomer, AccountType.TFSA, "CAD");

        assertThatThrownBy(() -> service.open(activeCustomer, AccountType.CHEQUING, "USD"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("maximum of 3");

        // Closing an account frees a slot.
        service.changeStatus(first.id(), AccountStatus.CLOSED);
        Account fourth = service.open(activeCustomer, AccountType.CHEQUING, "USD");
        assertThat(fourth.currency().getCurrencyCode()).isEqualTo("USD");
    }

    @Test
    void accountNumbersAreUnique() {
        Account a = service.open(activeCustomer, AccountType.CHEQUING, "CAD");
        Account b = service.open(activeCustomer, AccountType.SAVINGS, "CAD");

        assertThat(a.accountNumber()).isNotEqualTo(b.accountNumber());
    }

    @Test
    void freezeThenUnfreeze() {
        Account account = service.open(activeCustomer, AccountType.CHEQUING, "CAD");

        assertThat(service.changeStatus(account.id(), AccountStatus.FROZEN).status())
                .isEqualTo(AccountStatus.FROZEN);
        assertThat(service.changeStatus(account.id(), AccountStatus.ACTIVE).status())
                .isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    void closedAccountCannotBeReopened() {
        Account account = service.open(activeCustomer, AccountType.CHEQUING, "CAD");
        service.changeStatus(account.id(), AccountStatus.CLOSED);

        assertThatThrownBy(() -> service.changeStatus(account.id(), AccountStatus.ACTIVE))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void listsAccountsPerCustomerOrAll() {
        Account a = service.open(activeCustomer, AccountType.CHEQUING, "CAD");
        Account b = service.open(activeCustomer, AccountType.SAVINGS, "CAD");

        assertThat(service.list(activeCustomer)).containsExactly(a, b);
        assertThat(service.list(null)).containsExactly(a, b);
        assertThat(service.list(UUID.randomUUID())).isEmpty();
    }
}
