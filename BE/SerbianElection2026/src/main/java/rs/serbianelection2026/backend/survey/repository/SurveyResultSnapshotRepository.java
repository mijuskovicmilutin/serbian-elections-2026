package rs.serbianelection2026.backend.survey.repository;

import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import rs.serbianelection2026.backend.survey.entity.SurveyResultSnapshot;

public interface SurveyResultSnapshotRepository extends JpaRepository<SurveyResultSnapshot, Long> {

    Optional<SurveyResultSnapshot> findFirstBySurveyIdOrderByComputedAtDesc(Long surveyId);

    long deleteBySurveyIdAndComputedAtBefore(Long surveyId, Instant before);
}
