package rs.serbianelection2026.backend.ingestion.predictionmarket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import rs.serbianelection2026.backend.ingestion.entity.DataImport;
import rs.serbianelection2026.backend.ingestion.entity.ImportStatus;

@Slf4j
@Component
@ConditionalOnProperty(name = "polymarket.import-enabled", havingValue = "true", matchIfMissing = true)
public class PolymarketImportJob {

    private final PolymarketImportService polymarketImportService;

    public PolymarketImportJob(PolymarketImportService polymarketImportService) {
        this.polymarketImportService = polymarketImportService;
    }

    @Scheduled(fixedRateString = "${polymarket.import-interval-ms:900000}")
    public void run() {
        log.info("Polymarket import job triggered");

        DataImport result = polymarketImportService.importMarket();
        if (result.getStatus() == ImportStatus.SUCCESS) {
            log.info("Polymarket import job finished successfully (dataImportId={})", result.getId());
        } else {
            log.error(
                    "Polymarket import job finished unsuccessfully (dataImportId={}): {}",
                    result.getId(),
                    result.getErrorMessage());
        }
    }
}
