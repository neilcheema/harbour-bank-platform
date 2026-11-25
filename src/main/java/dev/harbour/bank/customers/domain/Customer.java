package dev.harbour.bank.customers.domain;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * A bank customer. This is a plain Java class with no framework annotations: the business rules
 * live here and can be tested without Spring. In Week 2 it gains JPA mapping.
 */
public final class Customer {

    private final UUID id;
    private final String fullName;
    private final String email;
    private final Instant createdAt;
    private CustomerStatus status;

    private Customer(UUID id, String fullName, String email, Instant createdAt, CustomerStatus status) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.createdAt = createdAt;
        this.status = status;
    }

    /** Registers a new, active customer. Emails are stored trimmed and lower-case. */
    public static Customer register(String fullName, String email, Clock clock) {
        Objects.requireNonNull(fullName, "fullName");
        Objects.requireNonNull(email, "email");
        Objects.requireNonNull(clock, "clock");
        return new Customer(
                UUID.randomUUID(),
                fullName.strip(),
                email.strip().toLowerCase(Locale.ROOT),
                clock.instant(),
                CustomerStatus.ACTIVE);
    }

    public UUID id() {
        return id;
    }

    public String fullName() {
        return fullName;
    }

    public String email() {
        return email;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public CustomerStatus status() {
        return status;
    }

    public boolean isActive() {
        return status == CustomerStatus.ACTIVE;
    }

    public void suspend() {
        status = CustomerStatus.SUSPENDED;
    }

    public void reinstate() {
        status = CustomerStatus.ACTIVE;
    }
}
