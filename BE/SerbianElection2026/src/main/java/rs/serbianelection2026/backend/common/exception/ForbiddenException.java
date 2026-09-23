package rs.serbianelection2026.backend.common.exception;

/** The request was understood but is refused (for example a failed bot check). */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
