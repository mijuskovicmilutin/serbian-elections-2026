package rs.serbianelection2026.backend.poll.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Everything about a poll for the review screen, including review state, the source snapshot and history. */
public record AdminPollResponse(
        Long id,
        PollsterRefResponse pollster,
        String status,
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
        Instant reviewedAt,
        String reviewNote,
        Instant scrapedAt,
        String contentHash,
        String sourceSnapshot,
        List<AdminPollResultResponse> results,
        PollReadiness readiness,
        List<PollAuditEntryResponse> audit) {
}
