package dev.harbour.bank.accounts.domain;

import dev.harbour.bank.shared.error.BusinessRuleException;
import java.time.Clock;
import java.time.Instant;
import java.util.Currency;
import java.util.Objects;
import java.util.UUID;

/**
 * A customer's bank account. Balances arrive in Week 3 with the ledger, so for now an account only
 * has identity, type, currency and a lifecycle status.
 */
public final class Account {

    private final UUID id;
    private final UUID customerId;
    private final String accountNumber;
    private final AccountType type;
    private final Currency currency;
    private final Instant createdAt;
    private AccountStatus status;

    private Account(
            UUID id,
            UUID customerId,
            String accountNumber,
            AccountType type,
            Currency currency,
            Instant createdAt,
            AccountStatus status) {
        this.id = id;
        this.customerId = customerId;
        this.accountNumber = accountNumber;
        this.type = type;
        this.currency = currency;
        this.createdAt = createdAt;
        this.status = status;
    }

    public static Account open(
            UUID customerId, String accountNumber, AccountType type, Currency currency, Clock clock) {
        Objects.requireNonNull(customerId, "customerId");
        Objects.requireNonNull(accountNumber, "accountNumber");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(currency, "currency");
        Objects.requireNonNull(clock, "clock");
        return new Account(
                UUID.randomUUID(), customerId, accountNumber, type, currency, clock.instant(), AccountStatus.ACTIVE);
    }

    /**
     * Moves the account to a new status if the lifecycle allows it.
     *
     * <p>Week 3 adds one more rule: an account can only be closed when its balance is zero.
     */
    public void changeStatus(AccountStatus target) {
        if (!status.canTransitionTo(target)) {
            throw new BusinessRuleException(
                    "Account %s cannot change status from %s to %s".formatted(accountNumber, status, target));
        }
        status = target;
    }

    public UUID id() {
        return id;
    }

    public UUID customerId() {
        return customerId;
    }

    public String accountNumber() {
        return accountNumber;
    }

    public AccountType type() {
        return type;
    }

    public Currency currency() {
        return currency;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public AccountStatus status() {
        return status;
    }
}
