package dev.harbour.bank.accounts.web;

import dev.harbour.bank.accounts.domain.AccountStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeStatusRequest(@NotNull AccountStatus status) {}
