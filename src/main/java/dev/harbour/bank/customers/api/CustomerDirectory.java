package dev.harbour.bank.customers.api;

import java.util.Optional;
import java.util.UUID;

/**
 * Public interface of the {@code customers} module. Other modules (such as {@code accounts}) depend
 * on this interface only, never on customer domain classes or repositories. An ArchUnit test in
 * Week 9 will enforce that rule.
 */
public interface CustomerDirectory {

    Optional<CustomerView> find(UUID customerId);
}
