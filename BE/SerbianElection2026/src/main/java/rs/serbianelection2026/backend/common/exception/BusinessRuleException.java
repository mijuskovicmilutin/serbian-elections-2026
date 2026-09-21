package rs.serbianelection2026.backend.common.exception;

/** The request is well-formed but violates a business rule (e.g. publishing an incomplete poll). */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
