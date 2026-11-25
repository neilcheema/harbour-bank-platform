package dev.harbour.bank.accounts.web;

import dev.harbour.bank.accounts.domain.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.UUID;

public record OpenAccountRequest(
        @NotNull UUID customerId,
        @NotNull AccountType type,
        @NotBlank @Pattern(regexp = "[A-Z]{3}", message = "must be a 3-letter uppercase currency code") String currency) {}
