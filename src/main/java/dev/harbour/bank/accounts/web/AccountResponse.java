package dev.harbour.bank.accounts.web;

import dev.harbour.bank.accounts.domain.Account;
import dev.harbour.bank.accounts.domain.AccountStatus;
import dev.harbour.bank.accounts.domain.AccountType;
import java.time.Instant;
import java.util.UUID;

public record AccountResponse(
        UUID id,
        UUID customerId,
        String accountNumber,
        AccountType type,
        String currency,
        AccountStatus status,
        Instant createdAt) {

    public static AccountResponse from(Account account) {
        return new AccountResponse(
                account.id(),
                account.customerId(),
                account.accountNumber(),
                account.type(),
                account.currency().getCurrencyCode(),
                account.status(),
                account.createdAt());
    }
}
