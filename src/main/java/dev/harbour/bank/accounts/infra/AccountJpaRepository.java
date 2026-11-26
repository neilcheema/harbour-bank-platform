package dev.harbour.bank.accounts.infra;

import dev.harbour.bank.accounts.domain.Account;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data repository for accounts; see {@code CustomerJpaRepository} for how these work. */
interface AccountJpaRepository extends JpaRepository<Account, UUID> {

    List<Account> findByCustomerIdOrderByCreatedAtAscAccountNumberAsc(UUID customerId);

    List<Account> findAllByOrderByCreatedAtAscAccountNumberAsc();
}
