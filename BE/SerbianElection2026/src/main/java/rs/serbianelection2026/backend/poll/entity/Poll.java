package rs.serbianelection2026.backend.poll.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.serbianelection2026.backend.common.entity.AuditableEntity;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "poll")
public class Poll extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pollster_id", nullable = false)
    private Pollster pollster;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(name = "published_at", nullable = false)
    private Instant publishedAt;

    @Column(name = "fieldwork_from")
    private LocalDate fieldworkFrom;

    @Column(name = "fieldwork_to")
    private LocalDate fieldworkTo;

    @Column(name = "fieldwork_note")
    private String fieldworkNote;

    @Column(name = "sample_size")
    private Integer sampleSize;

    private String population;

    private String method;

    @Column(name = "conducted_by")
    private String conductedBy;

    @Column(name = "commissioned_by")
    private String commissionedBy;

    @Column(name = "margin_of_error", precision = 5, scale = 2)
    private BigDecimal marginOfError;

    @Enumerated(EnumType.STRING)
    @Column(name = "result_basis", length = 30)
    private ResultBasis resultBasis;

    @Column(name = "decided_share_pct", precision = 5, scale = 2)
    private BigDecimal decidedSharePct;

    @Column(name = "undecided_pct", precision = 5, scale = 2)
    private BigDecimal undecidedPct;

    @Column(name = "wont_vote_pct", precision = 5, scale = 2)
    private BigDecimal wontVotePct;

    @Column(name = "will_vote_pct", precision = 5, scale = 2)
    private BigDecimal willVotePct;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_kind", nullable = false, length = 20)
    private SourceKind sourceKind;

    @Column(name = "source_url", nullable = false, length = 2048)
    private String sourceUrl;

    @Column(name = "original_document_url", length = 2048)
    private String originalDocumentUrl;

    /** JSON list of {@code {"name": ..., "url": ...}} media reports about this poll. */
    @Column(name = "media_sources", columnDefinition = "TEXT")
    private String mediaSources;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PollStatus status;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "review_note", columnDefinition = "TEXT")
    private String reviewNote;

    @Column(name = "scraped_at")
    private Instant scrapedAt;

    @Column(name = "content_hash", length = 64)
    private String contentHash;

    @Column(name = "source_snapshot", columnDefinition = "TEXT")
    private String sourceSnapshot;
}
