package dev.harbour.bank.shared.error;

/** The request conflicts with existing state (for example a duplicate email). Reported as HTTP 409. */
public final class ConflictException extends ApiException {

    public ConflictException(String message) {
        super(message);
    }
}
