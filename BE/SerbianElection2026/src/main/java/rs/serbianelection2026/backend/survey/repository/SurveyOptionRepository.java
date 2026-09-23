package rs.serbianelection2026.backend.survey.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import rs.serbianelection2026.backend.survey.entity.SurveyOption;

public interface SurveyOptionRepository extends JpaRepository<SurveyOption, Long> {

    List<SurveyOption> findByQuestionIdInOrderByPositionAsc(Collection<Long> questionIds);

    Optional<SurveyOption> findByQuestionIdAndElectoralListId(Long questionId, Long electoralListId);
}
