package rs.serbianelection2026.backend.survey.service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.serbianelection2026.backend.survey.dto.SurveyResultsResponse;
import rs.serbianelection2026.backend.survey.entity.PopulationMargin;
import rs.serbianelection2026.backend.survey.entity.Survey;
import rs.serbianelection2026.backend.survey.entity.SurveyAnswer;
import rs.serbianelection2026.backend.survey.entity.SurveyOption;
import rs.serbianelection2026.backend.survey.entity.SurveyQuestion;
import rs.serbianelection2026.backend.survey.entity.SurveyResultSnapshot;
import rs.serbianelection2026.backend.survey.entity.SurveyRole;
import rs.serbianelection2026.backend.survey.repository.PopulationMarginRepository;
import rs.serbianelection2026.backend.survey.repository.SurveyAnswerRepository;
import rs.serbianelection2026.backend.survey.repository.SurveyOptionRepository;
import rs.serbianelection2026.backend.survey.repository.SurveyQuestionRepository;
import rs.serbianelection2026.backend.survey.repository.SurveyResultSnapshotRepository;
import rs.serbianelection2026.backend.survey.weighting.RakingWeighter;
import rs.serbianelection2026.backend.survey.weighting.SurveyResultsCalculator;
import rs.serbianelection2026.backend.survey.weighting.SurveyResultsCalculator.CategoryInfo;
import rs.serbianelection2026.backend.survey.weighting.SurveyResultsCalculator.Input;
import rs.serbianelection2026.backend.survey.weighting.SurveyResultsCalculator.Meta;
import rs.serbianelection2026.backend.survey.weighting.SurveyResultsCalculator.OptionInfo;
import rs.serbianelection2026.backend.survey.weighting.SurveyResultsCalculator.Respondent;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/** Computes the results from the stored answers and keeps them as snapshots; the public API serves the latest one. */
@Slf4j
@Service
public class SurveyResultsService {

    private static final Duration KEEP_SNAPSHOTS = Duration.ofDays(2);

    private final SurveyQuestionRepository questionRepository;
    private final SurveyOptionRepository optionRepository;
    private final SurveyAnswerRepository answerRepository;
    private final PopulationMarginRepository marginRepository;
    private final SurveyResultSnapshotRepository snapshotRepository;
    private final ObjectMapper objectMapper;
    private final SurveyResultsCalculator calculator = new SurveyResultsCalculator(new RakingWeighter());

    public SurveyResultsService(
            SurveyQuestionRepository questionRepository,
            SurveyOptionRepository optionRepository,
            SurveyAnswerRepository answerRepository,
            PopulationMarginRepository marginRepository,
            SurveyResultSnapshotRepository snapshotRepository,
            ObjectMapper objectMapper) {
        this.questionRepository = questionRepository;
        this.optionRepository = optionRepository;
        this.answerRepository = answerRepository;
        this.marginRepository = marginRepository;
        this.snapshotRepository = snapshotRepository;
        this.objectMapper = objectMapper;
    }

    /** The latest snapshot; the first request after the survey was created computes one. */
    @Transactional
    public SurveyResultsResponse latest(Survey survey) {
        return snapshotRepository.findFirstBySurveyIdOrderByComputedAtDesc(survey.getId())
                .map(this::parse)
                .orElseGet(() -> computeAndStore(survey));
    }

    @Transactional
    public SurveyResultsResponse computeAndStore(Survey survey) {
        SurveyResultsResponse results = calculate(survey, Instant.now());
        try {
            snapshotRepository.save(SurveyResultSnapshot.builder()
                    .surveyId(survey.getId())
                    .computedAt(results.computedAt())
                    .responseCount(results.responseCount())
                    .payload(objectMapper.writeValueAsString(results))
                    .build());
        } catch (JacksonException e) {
            throw new IllegalStateException("Could not serialise survey results", e);
        }
        snapshotRepository.deleteBySurveyIdAndComputedAtBefore(survey.getId(), results.computedAt().minus(KEEP_SNAPSHOTS));
        log.info(
                "Survey {} snapshot stored: responses={}, weighted={}",
                survey.getId(),
                results.responseCount(),
                results.weightedAvailable());
        return results;
    }

    /** Computes the results now without storing them. */
    @Transactional(readOnly = true)
    public SurveyResultsResponse calculate(Survey survey, Instant now) {
        List<SurveyQuestion> questions = questionRepository.findBySurveyIdOrderByPosition(survey.getId());
        Map<SurveyRole, SurveyQuestion> byRole = new EnumMap<>(SurveyRole.class);
        questions.forEach(q -> byRole.put(q.getRole(), q));

        List<SurveyOption> allOptions = optionRepository.findByQuestionIdInOrderByPositionAsc(
                questions.stream().map(SurveyQuestion::getId).toList());
        Map<Long, List<SurveyOption>> optionsByQuestion = new HashMap<>();
        Map<Long, SurveyOption> optionById = new HashMap<>();
        for (SurveyOption o : allOptions) {
            optionsByQuestion.computeIfAbsent(o.getQuestionId(), k -> new ArrayList<>()).add(o);
            optionById.put(o.getId(), o);
        }

        Map<String, Long> populationByCode = new HashMap<>();
        for (PopulationMargin m : marginRepository.findAll()) {
            populationByCode.put(m.getDimension() + "/" + m.getCategoryCode(), m.getPopulation());
        }

        Map<SurveyRole, List<CategoryInfo>> demographics = new EnumMap<>(SurveyRole.class);
        Map<SurveyRole, Map<Long, Integer>> categoryIndex = new EnumMap<>(SurveyRole.class);
        for (SurveyRole role : SurveyResultsCalculator.DIMENSIONS) {
            List<CategoryInfo> categories = new ArrayList<>();
            Map<Long, Integer> index = new HashMap<>();
            for (SurveyOption o : optionsByQuestion.getOrDefault(byRole.get(role).getId(), List.of())) {
                index.put(o.getId(), categories.size());
                categories.add(new CategoryInfo(o.getCode(), o.getLabel(), populationByCode.get(role.name() + "/" + o.getCode())));
            }
            demographics.put(role, categories);
            categoryIndex.put(role, index);
        }

        Long voteQuestion = byRole.get(SurveyRole.VOTE_INTENTION).getId();
        Long turnoutQuestion = byRole.get(SurveyRole.TURNOUT).getId();

        Map<Long, Map<Long, Long>> answersByResponse = new HashMap<>();
        for (SurveyAnswer a : answerRepository.findAllOfActiveResponses(survey.getId())) {
            answersByResponse
                    .computeIfAbsent(a.getId().getResponseId(), k -> new HashMap<>())
                    .put(a.getId().getQuestionId(), a.getOptionId());
        }

        List<Respondent> respondents = new ArrayList<>();
        for (Map<Long, Long> answers : answersByResponse.values()) {
            Long vote = answers.get(voteQuestion);
            Long turnout = answers.get(turnoutQuestion);
            if (vote == null || turnout == null || !optionById.containsKey(turnout)) {
                continue;
            }
            int[] categories = new int[SurveyResultsCalculator.DIMENSIONS.size()];
            boolean complete = true;
            for (int d = 0; d < categories.length; d++) {
                SurveyRole role = SurveyResultsCalculator.DIMENSIONS.get(d);
                Long option = answers.get(byRole.get(role).getId());
                Integer index = option == null ? null : categoryIndex.get(role).get(option);
                if (index == null) {
                    complete = false;
                    break;
                }
                categories[d] = index;
            }
            if (complete) {
                respondents.add(new Respondent(vote, optionById.get(turnout).getCode(), categories));
            }
        }

        Input input = new Input(
                respondents,
                toInfo(optionsByQuestion.getOrDefault(voteQuestion, List.of())),
                toInfo(optionsByQuestion.getOrDefault(turnoutQuestion, List.of())),
                demographics,
                survey.getMinWeightedResponses());
        return calculator.compute(new Meta(survey.getId(), now, survey.getOpensAt(), survey.getClosesAt()), input);
    }

    private static List<OptionInfo> toInfo(List<SurveyOption> options) {
        return options.stream()
                .map(o -> new OptionInfo(o.getId(), o.getCode(), o.getLabel(), o.getPosition(), o.getKind(), o.isActive()))
                .toList();
    }

    private SurveyResultsResponse parse(SurveyResultSnapshot snapshot) {
        try {
            return objectMapper.readValue(snapshot.getPayload(), SurveyResultsResponse.class);
        } catch (JacksonException e) {
            throw new IllegalStateException("Stored survey snapshot " + snapshot.getId() + " is unreadable", e);
        }
    }
}
