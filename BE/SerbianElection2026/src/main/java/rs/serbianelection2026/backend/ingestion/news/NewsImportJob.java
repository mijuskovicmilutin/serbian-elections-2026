package rs.serbianelection2026.backend.ingestion.news;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import rs.serbianelection2026.backend.ingestion.entity.DataImport;
import rs.serbianelection2026.backend.ingestion.entity.ImportStatus;

@Slf4j
@Component
@ConditionalOnProperty(name = "news.import-enabled", havingValue = "true", matchIfMissing = true)
public class NewsImportJob {

    private final List<NewsProvider> providers;
    private final NewsImportService newsImportService;

    public NewsImportJob(List<NewsProvider> providers, NewsImportService newsImportService) {
        this.providers = providers;
        this.newsImportService = newsImportService;
    }

    @Scheduled(fixedRateString = "${news.import-interval-ms:900000}")
    public void run() {
        log.info("News import job triggered for {} provider(s)", providers.size());

        for (NewsProvider provider : providers) {
            DataImport result = newsImportService.importSource(provider);
            if (result.getStatus() == ImportStatus.SUCCESS) {
                log.info("{} news import finished successfully (dataImportId={})", provider.getSource(), result.getId());
            } else {
                log.error(
                        "{} news import finished unsuccessfully (dataImportId={}): {}",
                        provider.getSource(),
                        result.getId(),
                        result.getErrorMessage());
            }
        }
    }
}
