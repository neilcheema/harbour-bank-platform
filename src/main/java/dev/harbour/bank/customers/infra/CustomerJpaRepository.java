package dev.harbour.bank.customers.infra;

import dev.harbour.bank.customers.domain.Customer;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data repository: we declare only the interface and Spring generates the implementation at
 * startup. Query methods are derived from their names ({@code findByEmail} becomes
 * {@code select ... where email = ?}). Package-private on purpose: only the adapter next to it uses it.
 */
interface CustomerJpaRepository extends JpaRepository<Customer, UUID> {

    Optional<Customer> findByEmail(String email);

    List<Customer> findAllByOrderByCreatedAtAscIdAsc();
}
