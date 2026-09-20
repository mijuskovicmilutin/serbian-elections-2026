package rs.serbianelection2026.backend.poll.dto;

import java.math.BigDecimal;

/** {@code rawOptionName} is exactly what the source published; options are already in source order. */
public record PollResultResponse(String rawOptionName, BigDecimal percentage, int displayOrder) {
}
