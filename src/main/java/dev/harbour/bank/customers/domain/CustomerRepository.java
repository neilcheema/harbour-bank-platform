package dev.harbour.bank.customers.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Storage port for customers. Week 1 uses an in-memory implementation; Week 2 replaces it with
 * PostgreSQL through Spring Data JPA without changing any caller.
 */
public interface CustomerRepository {

    Customer save(Customer customer);

    Optional<Customer> findById(UUID id);

    Optional<Customer> findByEmail(String email);

    List<Customer> findAll();
}
