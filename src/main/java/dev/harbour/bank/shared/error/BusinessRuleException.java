package dev.harbour.bank.shared.error;

/**
 * The request is well-formed but a business rule forbids it (for example reopening a closed
 * account). Reported as HTTP 422.
 */
public final class BusinessRuleException extends ApiException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
