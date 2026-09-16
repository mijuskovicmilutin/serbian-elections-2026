package rs.serbianelection2026.backend.ingestion.rik;

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

    public RikImportJob(RikImportService rikImportService) {
        this.rikImportService = rikImportService;
    }

    @Scheduled(fixedRateString = "${rik.import-interval-ms:900000}")
    public void run() {
        log.info("Starting RIK electoral list import");
        DataImport result = rikImportService.importElectoralLists();
        if (result.getStatus() == ImportStatus.SUCCESS) {
            log.info(
                    "RIK import finished: found={}, created={}, updated={}",
                    result.getRecordsFound(),
                    result.getRecordsCreated(),
                    result.getRecordsUpdated());
        } else {
            log.error("RIK import failed: {}", result.getErrorMessage());
        }
    }
}
