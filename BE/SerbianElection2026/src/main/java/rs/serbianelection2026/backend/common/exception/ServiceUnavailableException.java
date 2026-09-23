package rs.serbianelection2026.backend.common.exception;

/** A feature is switched off because its configuration is missing (never opened by accident). */
public class ServiceUnavailableException extends RuntimeException {

    public ServiceUnavailableException(String message) {
        super(message);
    }
}
