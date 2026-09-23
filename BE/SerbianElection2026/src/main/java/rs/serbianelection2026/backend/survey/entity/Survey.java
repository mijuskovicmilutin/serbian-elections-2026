package rs.serbianelection2026.backend.survey.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
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
@Table(name = "survey")
public class Survey extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String slug;

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SurveyStatus status;

    @Column(name = "opens_at", nullable = false)
    private Instant opensAt;

    @Column(name = "closes_at", nullable = false)
    private Instant closesAt;

    /** Below this many answers only the raw view is available. */
    @Column(name = "min_weighted_responses", nullable = false)
    private int minWeightedResponses;

    /** How many answers a single (hashed) network may send. */
    @Column(name = "max_per_network", nullable = false)
    private int maxPerNetwork;

    /** Takes answers: switched on, inside the window. */
    public boolean isAcceptingAnswers(Instant now) {
        return status == SurveyStatus.OPEN && !now.isBefore(opensAt) && now.isBefore(closesAt);
    }

    /** Public results are served from the moment the survey opens until it closes (election silence). */
    public boolean isShowingResults(Instant now) {
        return status != SurveyStatus.DRAFT && !now.isBefore(opensAt) && now.isBefore(closesAt);
    }
}
