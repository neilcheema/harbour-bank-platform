package dev.harbour.bank.accounts.infra;

import dev.harbour.bank.accounts.domain.Account;
import dev.harbour.bank.accounts.domain.AccountRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/** Adapter that implements the domain's {@link AccountRepository} port with Spring Data JPA. */
@Repository
public class JpaAccountRepository implements AccountRepository {

    private final AccountJpaRepository jpa;

    public JpaAccountRepository(AccountJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Account save(Account account) {
        return jpa.save(account);
    }

    @Override
    public Optional<Account> findById(UUID id) {
        return jpa.findById(id);
    }

    @Override
    public List<Account> findByCustomerId(UUID customerId) {
        return jpa.findByCustomerIdOrderByCreatedAtAscAccountNumberAsc(customerId);
    }

    @Override
    public List<Account> findAll() {
        return jpa.findAllByOrderByCreatedAtAscAccountNumberAsc();
    }
}
