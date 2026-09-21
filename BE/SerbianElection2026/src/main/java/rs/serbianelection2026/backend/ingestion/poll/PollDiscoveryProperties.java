package rs.serbianelection2026.backend.ingestion.poll;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "polls.discovery")
public class PollDiscoveryProperties {

    /** Candidates older than this are ignored, so the first run does not flood review with a feed's backlog. */
    private int maxAgeDays = 14;

    private String userAgent = "Mozilla/5.0 (compatible; izbori2026-bot/1.0)";
    private String crtaFeedUrl = "https://crta.rs/feed/";
    private String cesidFeedUrl = "https://www.cesid.rs/feed/";
}
