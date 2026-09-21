package rs.serbianelection2026.backend.poll.dto;

/** Non-blocking observation shown during review; {@code value} carries the number the UI needs to word it. */
public record PollWarning(String code, String value) {
}
