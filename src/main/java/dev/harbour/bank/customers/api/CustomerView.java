package dev.harbour.bank.customers.api;

import java.util.UUID;

/**
 * The slice of a customer that other modules are allowed to see. Everything else about a customer
 * stays private to the {@code customers} module.
 */
public record CustomerView(UUID id, boolean active) {}
