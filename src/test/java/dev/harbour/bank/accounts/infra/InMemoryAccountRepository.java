package dev.harbour.bank.accounts.infra;

import dev.harbour.bank.accounts.domain.Account;
import dev.harbour.bank.accounts.domain.AccountRepository;
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
public class InMemoryAccountRepository implements AccountRepository {

    private static final Comparator<Account> BY_CREATION =
            Comparator.comparing(Account::createdAt).thenComparing(Account::accountNumber);

    private final Map<UUID, Account> store = new ConcurrentHashMap<>();

    @Override
    public Account save(Account account) {
        store.put(account.id(), account);
        return account;
    }

    @Override
    public Optional<Account> findById(UUID id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Account> findByCustomerId(UUID customerId) {
        return store.values().stream()
                .filter(a -> a.customerId().equals(customerId))
                .sorted(BY_CREATION)
                .toList();
    }

    @Override
    public List<Account> findAll() {
        return store.values().stream().sorted(BY_CREATION).toList();
    }
}
