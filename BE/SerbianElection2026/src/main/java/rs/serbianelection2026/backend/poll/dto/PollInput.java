package rs.serbianelection2026.backend.poll.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import rs.serbianelection2026.backend.poll.entity.ResultBasis;
import rs.serbianelection2026.backend.poll.entity.SourceKind;

/**
 * Full content of a poll as edited in review (used for both create and replace). Only http(s) URLs
 * are accepted because they are rendered as links on the public site.
 */
public record PollInput(
        @NotBlank String pollsterSlug,
        @NotBlank @Size(max = 500) String title,
        @NotNull Instant publishedAt,
        LocalDate fieldworkFrom,
        LocalDate fieldworkTo,
        @Size(max = 255) String fieldworkNote,
        @Positive Integer sampleSize,
        @Size(max = 255) String population,
        @Size(max = 255) String method,
        @Size(max = 255) String conductedBy,
        @Size(max = 255) String commissionedBy,
        @DecimalMin("0") @DecimalMax("100") BigDecimal marginOfError,
        ResultBasis resultBasis,
        @DecimalMin("0") @DecimalMax("100") BigDecimal decidedSharePct,
        @DecimalMin("0") @DecimalMax("100") BigDecimal undecidedPct,
        @DecimalMin("0") @DecimalMax("100") BigDecimal wontVotePct,
        @DecimalMin("0") @DecimalMax("100") BigDecimal willVotePct,
        @NotNull SourceKind sourceKind,
        @NotBlank @Size(max = 2048) @Pattern(regexp = "^https?://\\S+$", message = "must be an http(s) URL") String sourceUrl,
        @Size(max = 2048) @Pattern(regexp = "^(https?://\\S+)?$", message = "must be an http(s) URL") String originalDocumentUrl,
        @Size(max = 1000) String sourceNote,
        @Valid @Size(max = 20) List<MediaSourceInput> mediaSources,
        @Valid @Size(max = 50) List<PollResultInput> results) {
}
