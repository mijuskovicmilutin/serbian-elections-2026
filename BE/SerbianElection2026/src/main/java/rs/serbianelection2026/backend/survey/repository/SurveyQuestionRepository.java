package rs.serbianelection2026.backend.survey.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import rs.serbianelection2026.backend.survey.entity.SurveyQuestion;

public interface SurveyQuestionRepository extends JpaRepository<SurveyQuestion, Long> {

    List<SurveyQuestion> findBySurveyIdOrderByPosition(Long surveyId);
}
