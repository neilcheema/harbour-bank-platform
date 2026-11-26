package dev.harbour.bank.accounts.application;

/**
 * Issues unique account numbers. An interface so the service does not care where numbers come from:
 * production uses a PostgreSQL sequence, unit tests use a simple counter (a lambda is enough).
 */
@FunctionalInterface
public interface AccountNumberGenerator {

    String next();
}
