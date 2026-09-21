package rs.serbianelection2026.backend.common.exception;

/** The request references something invalid (unknown id, inconsistent values). */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
