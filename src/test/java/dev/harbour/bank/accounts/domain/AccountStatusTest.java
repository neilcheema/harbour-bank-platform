package dev.harbour.bank.accounts.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.harbour.bank.shared.error.BusinessRuleException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Currency;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class AccountStatusTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);

    /** One test method, nine cases: the full transition table from the AccountStatus Javadoc. */
    @ParameterizedTest(name = "{0} -> {1} allowed={2}")
    @CsvSource({
        "ACTIVE, FROZEN, true",
        "ACTIVE, CLOSED, true",
        "ACTIVE, ACTIVE, false",
        "FROZEN, ACTIVE, true",
        "FROZEN, CLOSED, true",
        "FROZEN, FROZEN, false",
        "CLOSED, ACTIVE, false",
        "CLOSED, FROZEN, false",
        "CLOSED, CLOSED, false"
    })
    void transitionTable(AccountStatus from, AccountStatus to, boolean allowed) {
        assertThat(from.canTransitionTo(to)).isEqualTo(allowed);
    }

    @Test
    void accountEnforcesTheLifecycle() {
        Account account = Account.open(
                UUID.randomUUID(), "HB-0000000001", AccountType.CHEQUING, Currency.getInstance("CAD"), CLOCK);
        assertThat(account.status()).isEqualTo(AccountStatus.ACTIVE);

        account.changeStatus(AccountStatus.CLOSED);
        assertThat(account.status()).isEqualTo(AccountStatus.CLOSED);

        assertThatThrownBy(() -> account.changeStatus(AccountStatus.ACTIVE))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("CLOSED")
                .hasMessageContaining("ACTIVE");
        assertThat(account.status()).isEqualTo(AccountStatus.CLOSED);
    }
}
