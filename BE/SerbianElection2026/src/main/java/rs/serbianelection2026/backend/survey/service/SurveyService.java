package rs.serbianelection2026.backend.survey.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.serbianelection2026.backend.common.exception.BadRequestException;
import rs.serbianelection2026.backend.common.exception.BusinessRuleException;
import rs.serbianelection2026.backend.common.exception.ForbiddenException;
import rs.serbianelection2026.backend.common.exception.NotFoundException;
import rs.serbianelection2026.backend.common.exception.ServiceUnavailableException;
import rs.serbianelection2026.backend.common.exception.TooManyRequestsException;
import rs.serbianelection2026.backend.survey.dto.AnswerInput;
import rs.serbianelection2026.backend.survey.dto.SubmitAnswersRequest;
import rs.serbianelection2026.backend.survey.dto.SurveyDetailResponse;
import rs.serbianelection2026.backend.survey.dto.SurveyResultsResponse;
import rs.serbianelection2026.backend.survey.entity.Survey;
import rs.serbianelection2026.backend.survey.entity.SurveyAnswer;
import rs.serbianelection2026.backend.survey.entity.SurveyAnswerId;
import rs.serbianelection2026.backend.survey.entity.SurveyOption;
import rs.serbianelection2026.backend.survey.entity.SurveyQuestion;
import rs.serbianelection2026.backend.survey.entity.SurveyResponse;
import rs.serbianelection2026.backend.survey.entity.SurveyStatus;
import rs.serbianelection2026.backend.survey.mapper.SurveyMapper;
import rs.serbianelection2026.backend.survey.repository.SurveyAnswerRepository;
import rs.serbianelection2026.backend.survey.repository.SurveyOptionRepository;
import rs.serbianelection2026.backend.survey.repository.SurveyQuestionRepository;
import rs.serbianelection2026.backend.survey.repository.SurveyRepository;
import rs.serbianelection2026.backend.survey.repository.SurveyResponseRepository;

/**
 * Public use cases of the visitor survey. Answering is anonymous: the visitor's address is used for the in-memory
 * rate limit and hashed for the per-network guard, and is never stored or logged.
 */
@Slf4j
@Service
public class SurveyService {

    private static final ZoneId BELGRADE = ZoneId.of("Europe/Belgrade");

    private final SurveyRepository surveyRepository;
    private final SurveyQuestionRepository questionRepository;
    private final SurveyOptionRepository optionRepository;
    private final SurveyResponseRepository responseRepository;
    private final SurveyAnswerRepository answerRepository;
    private final SurveyDedupeStore dedupeStore;
    private final NetworkHasher networkHasher;
    private final SubmissionRateLimiter rateLimiter;
    private final TurnstileVerifier turnstileVerifier;
    private final SurveyResultsService resultsService;
    private final SurveyMapper mapper;

    public SurveyService(
            SurveyRepository surveyRepository,
            SurveyQuestionRepository questionRepository,
            SurveyOptionRepository optionRepository,
            SurveyResponseRepository responseRepository,
            SurveyAnswerRepository answerRepository,
            SurveyDedupeStore dedupeStore,
            NetworkHasher networkHasher,
            SubmissionRateLimiter rateLimiter,
            TurnstileVerifier turnstileVerifier,
            SurveyResultsService resultsService,
            SurveyMapper mapper) {
        this.surveyRepository = surveyRepository;
        this.questionRepository = questionRepository;
        this.optionRepository = optionRepository;
        this.responseRepository = responseRepository;
        this.answerRepository = answerRepository;
        this.dedupeStore = dedupeStore;
        this.networkHasher = networkHasher;
        this.rateLimiter = rateLimiter;
        this.turnstileVerifier = turnstileVerifier;
        this.resultsService = resultsService;
        this.mapper = mapper;
    }

    /** The survey the site is running: the open one, else the most recent closed one. */
    @Transactional(readOnly = true)
    public SurveyDetailResponse getCurrent() {
        Survey survey = surveyRepository.findFirstByStatusOrderByOpensAtDesc(SurveyStatus.OPEN)
                .or(() -> surveyRepository.findFirstByStatusOrderByOpensAtDesc(SurveyStatus.CLOSED))
                .orElseThrow(() -> new NotFoundException("There is no survey"));
        return detail(survey);
    }

    @Transactional(readOnly = true)
    public SurveyDetailResponse getDetail(Long id) {
        return detail(load(id));
    }

    private SurveyDetailResponse detail(Survey survey) {
        Instant now = Instant.now();
        List<SurveyQuestion> questions = questionRepository.findBySurveyIdOrderByPosition(survey.getId());
        List<SurveyOption> options = optionRepository.findByQuestionIdInOrderByPositionAsc(
                questions.stream().map(SurveyQuestion::getId).toList());
        return mapper.toDetail(
                survey,
                questions,
                options,
                survey.isAcceptingAnswers(now),
                survey.isShowingResults(now),
                responseRepository.countBySurveyIdAndExcludedFalse(survey.getId()),
                turnstileVerifier.isEnabled());
    }

    /** Latest results snapshot; hidden once the survey is closed (election silence) or before it opens. */
    @Transactional
    public SurveyResultsResponse getResults(Long id) {
        Survey survey = load(id);
        if (!survey.isShowingResults(Instant.now())) {
            throw new NotFoundException("Survey results are not available");
        }
        return resultsService.latest(survey);
    }

    @Transactional
    public void submit(Long surveyId, SubmitAnswersRequest request, String clientAddress) {
        Survey survey = load(surveyId);
        if (!survey.isAcceptingAnswers(Instant.now())) {
            throw new BusinessRuleException("The survey is not accepting answers");
        }
        if (!networkHasher.isConfigured()) {
            throw new ServiceUnavailableException("Survey submissions are not configured");
        }
        if (!rateLimiter.tryAcquire(clientAddress)) {
            throw new TooManyRequestsException("Too many answers from this address, try again later");
        }
        if (turnstileVerifier.isEnabled() && !turnstileVerifier.verify(request.turnstileToken())) {
            throw new ForbiddenException("The bot check failed");
        }

        Map<Long, Long> answerByQuestion = validate(survey, request.answers());

        String networkHash = networkHasher.hash(survey.getId(), clientAddress);
        if (!dedupeStore.tryCount(survey.getId(), networkHash, survey.getMaxPerNetwork())) {
            throw new TooManyRequestsException("The maximum number of answers from this network has been reached");
        }

        SurveyResponse response = responseRepository.save(SurveyResponse.builder()
                .surveyId(survey.getId())
                .submittedOn(LocalDate.now(BELGRADE))
                .excluded(false)
                .build());
        answerByQuestion.forEach((questionId, optionId) ->
                answerRepository.save(new SurveyAnswer(new SurveyAnswerId(response.getId(), questionId), optionId)));
        log.info("Survey answer accepted: surveyId={}", survey.getId());
    }

    /** Exactly one valid, active option for every question; anything else is a bad request. */
    private Map<Long, Long> validate(Survey survey, List<AnswerInput> answers) {
        List<SurveyQuestion> questions = questionRepository.findBySurveyIdOrderByPosition(survey.getId());
        Map<Long, SurveyOption> optionById = new HashMap<>();
        for (SurveyOption o : optionRepository.findByQuestionIdInOrderByPositionAsc(
                questions.stream().map(SurveyQuestion::getId).toList())) {
            optionById.put(o.getId(), o);
        }
        Set<Long> questionIds = new HashSet<>();
        questions.forEach(q -> questionIds.add(q.getId()));

        Map<Long, Long> chosen = new HashMap<>();
        for (AnswerInput answer : answers) {
            if (!questionIds.contains(answer.questionId())) {
                throw new BadRequestException("Unknown question: " + answer.questionId());
            }
            SurveyOption option = optionById.get(answer.optionId());
            if (option == null || !option.getQuestionId().equals(answer.questionId()) || !option.isActive()) {
                throw new BadRequestException("Unknown or unavailable option for question " + answer.questionId());
            }
            if (chosen.put(answer.questionId(), answer.optionId()) != null) {
                throw new BadRequestException("Question " + answer.questionId() + " was answered more than once");
            }
        }
        if (chosen.size() != questionIds.size()) {
            throw new BadRequestException("Every question must be answered");
        }
        return chosen;
    }

    private Survey load(Long id) {
        return surveyRepository.findById(id).orElseThrow(() -> new NotFoundException("Unknown survey: " + id));
    }
}
