package rs.serbianelection2026.backend.ingestion.poll;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import rs.serbianelection2026.backend.ingestion.entity.ImportSource;
import rs.serbianelection2026.backend.ingestion.news.RssFeedParser;

@Configuration
@EnableConfigurationProperties(PollDiscoveryProperties.class)
public class PollDiscoveryConfig {

    @Bean
    PollDiscoveryFeed crtaFeed(RestClient.Builder builder, RssFeedParser parser, PollDiscoveryProperties properties) {
        return new PollsterOwnFeed(ImportSource.POLL_CRTA, "crta", properties.getCrtaFeedUrl(), client(builder, properties), parser);
    }

    @Bean
    PollDiscoveryFeed cesidFeed(RestClient.Builder builder, RssFeedParser parser, PollDiscoveryProperties properties) {
        return new PollsterOwnFeed(ImportSource.POLL_CESID, "cesid", properties.getCesidFeedUrl(), client(builder, properties), parser);
    }

    /** Pollster sites sit behind bot protection, so we identify ourselves plainly instead of using a library default. */
    private RestClient client(RestClient.Builder builder, PollDiscoveryProperties properties) {
        return builder.clone().defaultHeader("User-Agent", properties.getUserAgent()).build();
    }
}
