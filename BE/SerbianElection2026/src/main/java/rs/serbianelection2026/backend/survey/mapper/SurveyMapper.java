package rs.serbianelection2026.backend.survey.mapper;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import rs.serbianelection2026.backend.survey.dto.SurveyDetailResponse;
import rs.serbianelection2026.backend.survey.dto.SurveyOptionResponse;
import rs.serbianelection2026.backend.survey.dto.SurveyQuestionResponse;
import rs.serbianelection2026.backend.survey.entity.Survey;
import rs.serbianelection2026.backend.survey.entity.SurveyOption;
import rs.serbianelection2026.backend.survey.entity.SurveyQuestion;

@Component
public class SurveyMapper {

    /** Only active options are offered; {@code options} must already be in position order. */
    public SurveyDetailResponse toDetail(
            Survey survey,
            List<SurveyQuestion> questions,
            List<SurveyOption> options,
            boolean acceptingAnswers,
            boolean resultsVisible,
            long responseCount,
            boolean botCheckRequired) {
        Map<Long, List<SurveyOption>> byQuestion = options.stream()
                .filter(SurveyOption::isActive)
                .collect(Collectors.groupingBy(SurveyOption::getQuestionId));
        List<SurveyQuestionResponse> questionResponses = questions.stream()
                .map(q -> new SurveyQuestionResponse(
                        q.getId(),
                        q.getPosition(),
                        q.getRole().name(),
                        q.getText(),
                        byQuestion.getOrDefault(q.getId(), List.of()).stream()
                                .map(o -> new SurveyOptionResponse(
                                        o.getId(), o.getPosition(), o.getLabel(), o.getKind().name(), o.getAddedOn()))
                                .toList()))
                .toList();
        return new SurveyDetailResponse(
                survey.getId(),
                survey.getSlug(),
                survey.getTitle(),
                survey.getOpensAt(),
                survey.getClosesAt(),
                acceptingAnswers,
                resultsVisible,
                responseCount,
                survey.getMinWeightedResponses(),
                botCheckRequired,
                questionResponses);
    }
}
