package rs.serbianelection2026.backend.ingestion.poll;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import rs.serbianelection2026.backend.ingestion.entity.DataImport;
import rs.serbianelection2026.backend.ingestion.entity.ImportStatus;

@Slf4j
@Component
@ConditionalOnProperty(name = "polls.discovery.enabled", havingValue = "true", matchIfMissing = true)
public class PollDiscoveryJob {

    private final List<PollDiscoveryFeed> feeds;
    private final PollDiscoveryService discoveryService;

    public PollDiscoveryJob(List<PollDiscoveryFeed> feeds, PollDiscoveryService discoveryService) {
        this.feeds = feeds;
        this.discoveryService = discoveryService;
    }

    @Scheduled(fixedRateString = "${polls.discovery.interval-ms:900000}", initialDelayString = "${polls.discovery.initial-delay-ms:60000}")
    public void run() {
        log.info("Poll discovery job triggered for {} feed(s)", feeds.size());
        for (PollDiscoveryFeed feed : feeds) {
            DataImport result = discoveryService.discover(feed);
            if (result.getStatus() == ImportStatus.SUCCESS) {
                log.info("Poll discovery {} finished successfully (dataImportId={})", feed.source(), result.getId());
            } else {
                log.error("Poll discovery {} finished unsuccessfully (dataImportId={}): {}", feed.source(), result.getId(), result.getErrorMessage());
            }
        }
    }
}
