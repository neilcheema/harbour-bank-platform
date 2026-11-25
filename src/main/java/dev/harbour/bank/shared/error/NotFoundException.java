package dev.harbour.bank.shared.error;

/** The requested resource does not exist. Reported as HTTP 404. */
public final class NotFoundException extends ApiException {

    public NotFoundException(String resource, Object id) {
        super("%s not found: %s".formatted(resource, id));
    }
}
