package dev.harbour.bank.customers.infra;

import dev.harbour.bank.customers.domain.Customer;
import dev.harbour.bank.customers.domain.CustomerRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/**
 * Adapter that implements the domain's {@link CustomerRepository} port with Spring Data JPA. This is
 * the Week 1 in-memory store's replacement: services and controllers did not change at all.
 */
@Repository
public class JpaCustomerRepository implements CustomerRepository {

    private final CustomerJpaRepository jpa;

    public JpaCustomerRepository(CustomerJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Customer save(Customer customer) {
        return jpa.save(customer);
    }

    @Override
    public Optional<Customer> findById(UUID id) {
        return jpa.findById(id);
    }

    @Override
    public Optional<Customer> findByEmail(String email) {
        return jpa.findByEmail(email);
    }

    @Override
    public List<Customer> findAll() {
        return jpa.findAllByOrderByCreatedAtAscIdAsc();
    }
}
