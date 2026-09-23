package rs.serbianelection2026.backend.survey.service;

import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import rs.serbianelection2026.backend.survey.entity.Survey;
import rs.serbianelection2026.backend.survey.entity.SurveyStatus;
import rs.serbianelection2026.backend.survey.repository.SurveyRepository;

/**
 * Every 15 minutes: bring the list options up to date, store a fresh results snapshot and, once a survey has closed
 * (election silence), remove its per-network hashes and stop publishing results.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "survey.jobs-enabled", havingValue = "true", matchIfMissing = true)
public class SurveyJob {

    private final SurveyRepository surveyRepository;
    private final SurveyOptionSyncService syncService;
    private final SurveyResultsService resultsService;
    private final SurveyDedupeStore dedupeStore;

    public SurveyJob(
            SurveyRepository surveyRepository,
            SurveyOptionSyncService syncService,
            SurveyResultsService resultsService,
            SurveyDedupeStore dedupeStore) {
        this.surveyRepository = surveyRepository;
        this.syncService = syncService;
        this.resultsService = resultsService;
        this.dedupeStore = dedupeStore;
    }

    @Scheduled(fixedRateString = "${survey.snapshot-interval-ms:900000}")
    public void run() {
        Instant now = Instant.now();
        for (Survey survey : surveyRepository.findAll()) {
            if (survey.getStatus() == SurveyStatus.DRAFT) {
                continue;
            }
            try {
                if (now.isBefore(survey.getClosesAt())) {
                    syncService.syncElectoralLists(survey);
                    resultsService.computeAndStore(survey);
                } else {
                    close(survey);
                }
            } catch (RuntimeException e) {
                log.error("Survey job failed for survey {}", survey.getId(), e);
            }
        }
    }

    private void close(Survey survey) {
        int removed = dedupeStore.deleteAll(survey.getId());
        if (survey.getStatus() != SurveyStatus.CLOSED) {
            survey.setStatus(SurveyStatus.CLOSED);
            surveyRepository.save(survey);
            log.info("Survey {} closed; removed {} network hashes", survey.getId(), removed);
        }
    }
}
