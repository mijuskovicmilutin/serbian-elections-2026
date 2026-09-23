package rs.serbianelection2026.backend.survey.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rs.serbianelection2026.backend.survey.entity.SurveyResponse;

public interface SurveyResponseRepository extends JpaRepository<SurveyResponse, Long> {

    long countBySurveyIdAndExcludedFalse(Long surveyId);

    long countBySurveyId(Long surveyId);
}
