package dev.harbour.bank.customers.application;

import dev.harbour.bank.customers.api.CustomerDirectory;
import dev.harbour.bank.customers.api.CustomerView;
import dev.harbour.bank.customers.domain.Customer;
import dev.harbour.bank.customers.domain.CustomerRepository;
import dev.harbour.bank.shared.error.ConflictException;
import dev.harbour.bank.shared.error.NotFoundException;
import java.time.Clock;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class CustomerService implements CustomerDirectory {

    private final CustomerRepository customers;
    private final Clock clock;

    // Constructor injection: Spring sees the single constructor and supplies both arguments.
    public CustomerService(CustomerRepository customers, Clock clock) {
        this.customers = customers;
        this.clock = clock;
    }

    public Customer register(String fullName, String email) {
        String normalizedEmail = email.strip().toLowerCase(Locale.ROOT);
        // Check-then-save is not safe under concurrent requests. Week 2 adds a unique constraint
        // in the database, which is the real guarantee.
        customers.findByEmail(normalizedEmail).ifPresent(existing -> {
            throw new ConflictException("A customer with this email already exists");
        });
        return customers.save(Customer.register(fullName, normalizedEmail, clock));
    }

    public Customer get(UUID id) {
        return customers.findById(id).orElseThrow(() -> new NotFoundException("Customer", id));
    }

    public List<Customer> list() {
        return customers.findAll();
    }

    @Override
    public Optional<CustomerView> find(UUID customerId) {
        return customers.findById(customerId).map(c -> new CustomerView(c.id(), c.isActive()));
    }
}
