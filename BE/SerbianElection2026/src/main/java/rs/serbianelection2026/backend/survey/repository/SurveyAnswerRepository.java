package rs.serbianelection2026.backend.survey.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import rs.serbianelection2026.backend.survey.entity.SurveyAnswer;
import rs.serbianelection2026.backend.survey.entity.SurveyAnswerId;

public interface SurveyAnswerRepository extends JpaRepository<SurveyAnswer, SurveyAnswerId> {

    /** Answers of every response of the survey that has not been excluded. */
    @Query("""
            SELECT a FROM SurveyAnswer a
            WHERE a.id.responseId IN (
                SELECT r.id FROM SurveyResponse r WHERE r.surveyId = :surveyId AND r.excluded = false)
            """)
    List<SurveyAnswer> findAllOfActiveResponses(@Param("surveyId") Long surveyId);
}
