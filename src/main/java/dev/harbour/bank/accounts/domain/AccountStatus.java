package dev.harbour.bank.accounts.domain;

/**
 * Account lifecycle. Allowed moves:
 *
 * <pre>
 *   ACTIVE  -> FROZEN, CLOSED
 *   FROZEN  -> ACTIVE, CLOSED
 *   CLOSED  -> (nothing: closing is final)
 * </pre>
 */
public enum AccountStatus {
    ACTIVE,
    FROZEN,
    CLOSED;

    public boolean canTransitionTo(AccountStatus target) {
        // Switch expression: it must produce a value for every enum constant, or it won't compile.
        return switch (this) {
            case ACTIVE -> target == FROZEN || target == CLOSED;
            case FROZEN -> target == ACTIVE || target == CLOSED;
            case CLOSED -> false;
        };
    }
}
