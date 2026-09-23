package rs.serbianelection2026.backend.survey.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import rs.serbianelection2026.backend.survey.entity.Survey;
import rs.serbianelection2026.backend.survey.entity.SurveyStatus;

public interface SurveyRepository extends JpaRepository<Survey, Long> {

    Optional<Survey> findBySlug(String slug);

    Optional<Survey> findFirstByStatusOrderByOpensAtDesc(SurveyStatus status);
}
