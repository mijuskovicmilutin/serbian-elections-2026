package rs.serbianelection2026.backend.poll.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Public view of an approved poll. Review state, source snapshot and the internal option
 * classification are deliberately not part of it. Unknown values are null, never guessed.
 */
public record PollResponse(
        Long id,
        PollsterRefResponse pollster,
        String title,
        Instant publishedAt,
        LocalDate fieldworkFrom,
        LocalDate fieldworkTo,
        String fieldworkNote,
        Integer sampleSize,
        String population,
        String method,
        String conductedBy,
        String commissionedBy,
        BigDecimal marginOfError,
        String resultBasis,
        BigDecimal decidedSharePct,
        BigDecimal undecidedPct,
        BigDecimal wontVotePct,
        BigDecimal willVotePct,
        String sourceKind,
        String sourceUrl,
        String originalDocumentUrl,
        String sourceNote,
        List<MediaSourceResponse> mediaSources,
        List<PollResultResponse> results) {
}
