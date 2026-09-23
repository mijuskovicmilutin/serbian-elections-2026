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

/** Results as computed at one moment; the public API only ever serves the latest one. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "survey_result_snapshot")
public class SurveyResultSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "survey_id", nullable = false)
    private Long surveyId;

    @Column(name = "computed_at", nullable = false)
    private Instant computedAt;

    @Column(name = "response_count", nullable = false)
    private int responseCount;

    /** JSON of {@code SurveyResultsResponse}. */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;
}
