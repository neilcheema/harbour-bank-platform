package dev.harbour.bank.customers.infra;

import dev.harbour.bank.customers.domain.Customer;
import dev.harbour.bank.customers.domain.CustomerRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory fake of the repository port, used by fast unit tests that need no database. It lives in
 * the test source set and is deliberately NOT a Spring bean: the application uses the JPA adapter.
 */
public class InMemoryCustomerRepository implements CustomerRepository {

    private final Map<UUID, Customer> store = new ConcurrentHashMap<>();

    @Override
    public Customer save(Customer customer) {
        store.put(customer.id(), customer);
        return customer;
    }

    @Override
    public Optional<Customer> findById(UUID id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public Optional<Customer> findByEmail(String email) {
        return store.values().stream().filter(c -> c.email().equals(email)).findFirst();
    }

    @Override
    public List<Customer> findAll() {
        return store.values().stream()
                .sorted(Comparator.comparing(Customer::createdAt).thenComparing(Customer::id))
                .toList();
    }
}
