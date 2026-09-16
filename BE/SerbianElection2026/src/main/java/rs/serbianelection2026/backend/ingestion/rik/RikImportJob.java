package rs.serbianelection2026.backend.ingestion.rik;

import java.time.LocalDate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import rs.serbianelection2026.backend.ingestion.entity.DataImport;
import rs.serbianelection2026.backend.ingestion.entity.ImportStatus;

@Slf4j
@Component
@ConditionalOnProperty(name = "rik.import-enabled", havingValue = "true", matchIfMissing = true)
public class RikImportJob {

    private final RikImportService rikImportService;
    private final RikProperties properties;

    public RikImportJob(RikImportService rikImportService, RikProperties properties) {
        this.rikImportService = rikImportService;
        this.properties = properties;
    }

    @Scheduled(fixedRateString = "${rik.import-interval-ms:900000}")
    public void run() {
        log.info("RIK import job triggered (deadline={})", properties.getElectoralListSubmissionDeadline());

        LocalDate deadline = properties.getElectoralListSubmissionDeadline();
        if (deadline != null && LocalDate.now().isAfter(deadline)) {
            log.info("Electoral list submission deadline ({}) has passed, skipping scheduled RIK import", deadline);
            return;
        }

        DataImport result = rikImportService.importElectoralLists();
        if (result.getStatus() == ImportStatus.SUCCESS) {
            log.info("RIK import job finished successfully (dataImportId={})", result.getId());
        } else {
            log.error(
                    "RIK import job finished unsuccessfully (dataImportId={}): {}",
                    result.getId(),
                    result.getErrorMessage());
        }
    }
}
