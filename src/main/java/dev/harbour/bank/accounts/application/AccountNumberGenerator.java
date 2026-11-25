package dev.harbour.bank.accounts.application;

import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

/**
 * Issues account numbers in a fictional scheme ("HB-" plus ten digits). Sequential and in-memory
 * for now; Week 2 will base it on a database sequence so numbers survive restarts.
 */
@Component
public class AccountNumberGenerator {

    private final AtomicLong next = new AtomicLong(1_000_000_000L);

    public String next() {
        return "HB-%010d".formatted(next.getAndIncrement());
    }
}
