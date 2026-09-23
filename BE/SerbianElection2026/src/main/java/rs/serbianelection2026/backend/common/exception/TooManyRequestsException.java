package rs.serbianelection2026.backend.common.exception;

/** The caller has sent too many requests (or answers) and must wait or stop. */
public class TooManyRequestsException extends RuntimeException {

    public TooManyRequestsException(String message) {
        super(message);
    }
}
