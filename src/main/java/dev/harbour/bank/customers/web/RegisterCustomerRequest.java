package dev.harbour.bank.customers.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request body for registering a customer. Validation annotations are checked by {@code @Valid}. */
public record RegisterCustomerRequest(
        @NotBlank @Size(max = 120) String fullName,
        @NotBlank @Email @Size(max = 254) String email) {}
