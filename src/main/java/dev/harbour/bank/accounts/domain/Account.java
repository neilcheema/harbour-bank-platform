package dev.harbour.bank.accounts.domain;

import dev.harbour.bank.shared.error.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Currency;
import java.util.Objects;
import java.util.UUID;

/**
 * A customer's bank account. Balances arrive in Week 3 with the ledger, so for now an account only
 * has identity, type, currency and a lifecycle status.
 *
 * <p>Mapped with JPA annotations (see ADR-011). The {@code version} field gives optimistic locking:
 * if two requests change the same account at once, the second UPDATE finds a newer version and fails
 * instead of silently overwriting the first.
 */
@Entity
@Table(name = "accounts")
public class Account {

    @Id
    private UUID id;

    @Version
    private Long version;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "account_number", nullable = false, length = 20)
    private String accountNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountType type;

    @Convert(converter = CurrencyConverter.class)
    @Column(nullable = false, length = 3)
    private Currency currency;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountStatus status;

    /** For JPA only. */
    protected Account() {}

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
                UUID.randomUUID(),
                customerId,
                accountNumber,
                type,
                currency,
                clock.instant().truncatedTo(ChronoUnit.MICROS),
                AccountStatus.ACTIVE);
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
