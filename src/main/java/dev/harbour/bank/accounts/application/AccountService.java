package dev.harbour.bank.accounts.application;

import dev.harbour.bank.accounts.domain.Account;
import dev.harbour.bank.accounts.domain.AccountRepository;
import dev.harbour.bank.accounts.domain.AccountStatus;
import dev.harbour.bank.accounts.domain.AccountType;
import dev.harbour.bank.customers.api.CustomerDirectory;
import dev.harbour.bank.customers.api.CustomerView;
import dev.harbour.bank.shared.error.BusinessRuleException;
import dev.harbour.bank.shared.error.NotFoundException;
import java.time.Clock;
import java.util.Currency;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {

    static final int MAX_OPEN_ACCOUNTS_PER_CUSTOMER = 3;
    private static final Set<String> SUPPORTED_CURRENCIES = Set.of("CAD", "USD");

    private final AccountRepository accounts;
    private final CustomerDirectory customers; // the customers module's public interface only
    private final AccountNumberGenerator numbers;
    private final Clock clock;

    public AccountService(
            AccountRepository accounts, CustomerDirectory customers, AccountNumberGenerator numbers, Clock clock) {
        this.accounts = accounts;
        this.customers = customers;
        this.numbers = numbers;
        this.clock = clock;
    }

    @Transactional
    public Account open(UUID customerId, AccountType type, String currencyCode) {
        CustomerView customer =
                customers.find(customerId).orElseThrow(() -> new NotFoundException("Customer", customerId));
        if (!customer.active()) {
            throw new BusinessRuleException("Customer is not active: " + customerId);
        }
        if (!SUPPORTED_CURRENCIES.contains(currencyCode)) {
            throw new BusinessRuleException("Unsupported currency: " + currencyCode);
        }

        // Stream pipeline (the Java counterpart of LINQ's Where(...).Count()).
        long openAccounts = accounts.findByCustomerId(customerId).stream()
                .filter(a -> a.status() != AccountStatus.CLOSED)
                .count();
        if (openAccounts >= MAX_OPEN_ACCOUNTS_PER_CUSTOMER) {
            throw new BusinessRuleException(
                    "Customer already has the maximum of %d open accounts".formatted(MAX_OPEN_ACCOUNTS_PER_CUSTOMER));
        }

        Account account =
                Account.open(customerId, numbers.next(), type, Currency.getInstance(currencyCode), clock);
        return accounts.save(account);
    }

    @Transactional(readOnly = true)
    public Account get(UUID id) {
        return accounts.findById(id).orElseThrow(() -> new NotFoundException("Account", id));
    }

    /** Lists a customer's accounts, or every account when {@code customerId} is null. */
    @Transactional(readOnly = true)
    public List<Account> list(UUID customerId) {
        return customerId == null ? accounts.findAll() : accounts.findByCustomerId(customerId);
    }

    /**
     * Inside this transaction the loaded account is "managed": Hibernate notices the status change and
     * writes it when the transaction commits, guarded by the version column (optimistic locking).
     */
    @Transactional
    public Account changeStatus(UUID id, AccountStatus target) {
        Account account = accounts.findById(id).orElseThrow(() -> new NotFoundException("Account", id));
        account.changeStatus(target);
        return accounts.save(account);
    }
}
