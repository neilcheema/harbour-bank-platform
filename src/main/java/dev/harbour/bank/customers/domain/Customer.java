package dev.harbour.bank.customers.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * A bank customer.
 *
 * <p>The business rules live here and are tested without Spring or a database. The class also carries
 * JPA mapping annotations (the standard {@code jakarta.persistence} API, not Spring) so it can be stored
 * directly; see ADR-011 for why we accepted that trade-off.
 *
 * <p>Because JPA sets fields by reflection, the fields are not {@code final} and there is a protected
 * no-argument constructor that only the persistence layer uses.
 */
@Entity
@Table(name = "customers")
public class Customer {

    @Id
    private UUID id;

    // Wrapper type on purpose: null means "never saved", which is how Spring Data decides between
    // INSERT and UPDATE for an entity whose id we assign ourselves.
    @Version
    private Long version;

    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName;

    @Column(nullable = false, length = 254)
    private String email;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CustomerStatus status;

    /** For JPA only. */
    protected Customer() {}

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
                // PostgreSQL stores microseconds; truncating here keeps the in-memory value identical
                // to what we read back later.
                clock.instant().truncatedTo(ChronoUnit.MICROS),
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
