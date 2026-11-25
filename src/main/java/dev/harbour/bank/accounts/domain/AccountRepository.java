package dev.harbour.bank.accounts.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Storage port for accounts. In-memory now, PostgreSQL in Week 2. */
public interface AccountRepository {

    Account save(Account account);

    Optional<Account> findById(UUID id);

    List<Account> findByCustomerId(UUID customerId);

    List<Account> findAll();
}
