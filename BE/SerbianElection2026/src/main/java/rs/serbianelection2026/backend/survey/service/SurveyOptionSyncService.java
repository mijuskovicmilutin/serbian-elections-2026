package rs.serbianelection2026.backend.survey.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.serbianelection2026.backend.election.entity.ElectoralList;
import rs.serbianelection2026.backend.election.entity.ElectoralListStatus;
import rs.serbianelection2026.backend.election.service.ElectionService;
import rs.serbianelection2026.backend.survey.entity.Survey;
import rs.serbianelection2026.backend.survey.entity.SurveyOption;
import rs.serbianelection2026.backend.survey.entity.SurveyOptionKind;
import rs.serbianelection2026.backend.survey.entity.SurveyQuestion;
import rs.serbianelection2026.backend.survey.entity.SurveyRole;
import rs.serbianelection2026.backend.survey.repository.SurveyOptionRepository;
import rs.serbianelection2026.backend.survey.repository.SurveyQuestionRepository;
import rs.serbianelection2026.backend.survey.repository.SurveyResponseRepository;

/**
 * Keeps the list options of the vote-intention question in step with the lists RIK has published: a new list is
 * added to the offer, a rejected or withdrawn list is switched off (and then neither offered nor counted).
 * Options are never deleted.
 */
@Slf4j
@Service
public class SurveyOptionSyncService {

    private static final ZoneId BELGRADE = ZoneId.of("Europe/Belgrade");

    private final ElectionService electionService;
    private final SurveyQuestionRepository questionRepository;
    private final SurveyOptionRepository optionRepository;
    private final SurveyResponseRepository responseRepository;

    public SurveyOptionSyncService(
            ElectionService electionService,
            SurveyQuestionRepository questionRepository,
            SurveyOptionRepository optionRepository,
            SurveyResponseRepository responseRepository) {
        this.electionService = electionService;
        this.questionRepository = questionRepository;
        this.optionRepository = optionRepository;
        this.responseRepository = responseRepository;
    }

    @Transactional
    public void syncElectoralLists(Survey survey) {
        SurveyQuestion question = questionRepository.findBySurveyIdOrderByPosition(survey.getId()).stream()
                .filter(q -> q.getRole() == SurveyRole.VOTE_INTENTION)
                .findFirst()
                .orElse(null);
        if (question == null) {
            log.warn("Survey {} has no vote-intention question, nothing to sync", survey.getId());
            return;
        }

        Map<Long, SurveyOption> byList = new HashMap<>();
        for (SurveyOption option : optionRepository.findByQuestionIdInOrderByPositionAsc(List.of(question.getId()))) {
            if (option.getElectoralListId() != null) {
                byList.put(option.getElectoralListId(), option);
            }
        }

        boolean answersExist = responseRepository.countBySurveyId(survey.getId()) > 0;
        int added = 0;
        int changed = 0;
        for (ElectoralList list : electionService.getCurrentElectoralLists()) {
            boolean active = list.getStatus() == ElectoralListStatus.SUBMITTED || list.getStatus() == ElectoralListStatus.PROCLAIMED;
            String label = ElectoralListLabels.displayName(list.getName());
            int position = list.getBallotNumber() != null ? list.getBallotNumber() : 500 + list.getId().intValue();
            SurveyOption option = byList.get(list.getId());

            if (option == null) {
                if (!active) {
                    continue;
                }
                optionRepository.save(SurveyOption.builder()
                        .questionId(question.getId())
                        .position(position)
                        .code("LIST_" + list.getId())
                        .label(label)
                        .kind(SurveyOptionKind.CHOICE)
                        .electoralListId(list.getId())
                        .active(true)
                        .addedOn(answersExist ? LocalDate.now(BELGRADE) : null)
                        .build());
                added++;
            } else if (option.isActive() != active || option.getPosition() != position || !option.getLabel().equals(label)) {
                option.setActive(active);
                option.setPosition(position);
                option.setLabel(label);
                optionRepository.save(option);
                changed++;
            }
        }
        if (added > 0 || changed > 0) {
            log.info("Survey {} list options synced: added={}, changed={}", survey.getId(), added, changed);
        }
    }
}
