package dev.harbour.bank.customers.web;

import dev.harbour.bank.customers.domain.Customer;
import dev.harbour.bank.customers.domain.CustomerStatus;
import java.time.Instant;
import java.util.UUID;

/** What the API returns for a customer. Domain objects are never exposed directly. */
public record CustomerResponse(
        UUID id, String fullName, String email, CustomerStatus status, Instant createdAt) {

    public static CustomerResponse from(Customer customer) {
        return new CustomerResponse(
                customer.id(), customer.fullName(), customer.email(), customer.status(), customer.createdAt());
    }
}
