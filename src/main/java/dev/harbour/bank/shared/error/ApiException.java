package dev.harbour.bank.shared.error;

/**
 * Base type for errors the API reports to clients as RFC 7807 problem details.
 *
 * <p>The class is {@code sealed}: the compiler knows the complete list of subclasses, so a
 * {@code switch} over an {@code ApiException} must cover every case (see
 * {@link ApiExceptionHandler}). Adding a new subclass here makes that switch fail to compile
 * until it is handled, which is exactly the safety net we want.
 */
public abstract sealed class ApiException extends RuntimeException
        permits NotFoundException, ConflictException, BusinessRuleException {

    protected ApiException(String message) {
        super(message);
    }
}
