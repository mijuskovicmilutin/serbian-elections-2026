package rs.serbianelection2026.backend.poll.dto;

import java.math.BigDecimal;

/** Review-side view of an option row, including the internal classification hidden from the public API. */
public record AdminPollResultResponse(
        String rawOptionName,
        BigDecimal percentage,
        int displayOrder,
        String optionKind,
        String composition,
        Long electoralListId) {
}
