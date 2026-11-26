package dev.harbour.bank.accounts.infra;

import dev.harbour.bank.accounts.application.AccountNumberGenerator;
import java.util.Objects;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Account numbers in a fictional scheme: "HB-" plus ten digits, taken from the {@code account_number_seq}
 * database sequence. The sequence is what makes numbers unique across restarts and across several
 * application instances.
 */
@Component
public class SequenceAccountNumberGenerator implements AccountNumberGenerator {

    private final JdbcTemplate jdbc;

    public SequenceAccountNumberGenerator(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public String next() {
        Long value = jdbc.queryForObject("select nextval('account_number_seq')", Long.class);
        return "HB-%010d".formatted(Objects.requireNonNull(value));
    }
}
