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
import org.springframework.transaction.annotation.Transactional;

/**
 * Customer use cases. {@code @Transactional} makes each public method run in one database
 * transaction: everything inside it commits together, or rolls back together if an exception escapes.
 * Read-only methods say so, which lets the database and Hibernate skip work they only need for writes.
 */
@Service
public class CustomerService implements CustomerDirectory {

    private final CustomerRepository customers;
    private final Clock clock;

    // Constructor injection: Spring sees the single constructor and supplies both arguments.
    public CustomerService(CustomerRepository customers, Clock clock) {
        this.customers = customers;
        this.clock = clock;
    }

    @Transactional
    public Customer register(String fullName, String email) {
        String normalizedEmail = email.strip().toLowerCase(Locale.ROOT);
        // This check gives a friendly error in the normal case. It is not the guarantee: two requests
        // can both pass it at the same moment. The UNIQUE constraint on customers.email is the real
        // protection, and ApiExceptionHandler turns that violation into a 409 as well.
        customers.findByEmail(normalizedEmail).ifPresent(existing -> {
            throw new ConflictException("A customer with this email already exists");
        });
        return customers.save(Customer.register(fullName, normalizedEmail, clock));
    }

    @Transactional(readOnly = true)
    public Customer get(UUID id) {
        return customers.findById(id).orElseThrow(() -> new NotFoundException("Customer", id));
    }

    @Transactional(readOnly = true)
    public List<Customer> list() {
        return customers.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CustomerView> find(UUID customerId) {
        return customers.findById(customerId).map(c -> new CustomerView(c.id(), c.isActive()));
    }
}
