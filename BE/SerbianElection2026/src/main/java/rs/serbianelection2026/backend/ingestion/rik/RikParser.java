package rs.serbianelection2026.backend.ingestion.rik;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import rs.serbianelection2026.backend.ingestion.rik.dto.NormalizedElectoralList;
import rs.serbianelection2026.backend.ingestion.rik.dto.RikDocumentRecord;

@Slf4j
@Component
public class RikParser {

    private final RikProperties properties;

    public RikParser(RikProperties properties) {
        this.properties = properties;
    }

    public List<NormalizedElectoralList> parse(List<RikDocumentRecord> records) {
        log.info("Parsing {} raw RIK record(s)", records.size());

        List<NormalizedElectoralList> result = new ArrayList<>();
        int skipped = 0;
        for (RikDocumentRecord record : records) {
            try {
                NormalizedElectoralList parsed = parseOne(record);
                log.debug(
                        "Parsed record: externalId={}, ballotNumber={}, name={}",
                        parsed.externalId(),
                        parsed.ballotNumber(),
                        parsed.name());
                result.add(parsed);
            } catch (RuntimeException e) {
                skipped++;
                log.warn("Skipping unparsable RIK record: {}", record, e);
            }
        }

        log.info("Parsed {} record(s) successfully, skipped {} unparsable record(s)", result.size(), skipped);
        return result;
    }

    private NormalizedElectoralList parseOne(RikDocumentRecord record) {
        String href = extractHref(record.extfilesLink());
        String externalId = extractExternalId(href);
        Integer ballotNumber = parseBallotNumber(record.number());
        Instant publishedAt = Instant.ofEpochSecond(Long.parseLong(record.datetime()));
        String sourceUrl = buildSourceUrl(href);
        String name = record.documentName() == null ? null : record.documentName().trim();
        return new NormalizedElectoralList(externalId, name, ballotNumber, sourceUrl, publishedAt);
    }

    private String extractHref(List<String> extfilesLink) {
        if (extfilesLink == null || extfilesLink.isEmpty()) {
            throw new IllegalArgumentException("Missing extfiles_link");
        }
        Element anchor = Jsoup.parseBodyFragment(extfilesLink.get(0)).selectFirst("a");
        if (anchor == null || anchor.attr("href").isBlank()) {
            throw new IllegalArgumentException("No usable <a href> in extfiles_link");
        }
        return anchor.attr("href");
    }

    private String extractExternalId(String href) {
        String[] segments = href.split("/");
        if (segments.length < 2) {
            throw new IllegalArgumentException("Unexpected href format: " + href);
        }
        return segments[segments.length - 2];
    }

    private Integer parseBallotNumber(String number) {
        if (number == null) {
            return null;
        }
        String cleaned = number.trim();
        if (cleaned.endsWith(".")) {
            cleaned = cleaned.substring(0, cleaned.length() - 1);
        }
        try {
            return Integer.parseInt(cleaned);
        } catch (NumberFormatException e) {
            log.warn("Could not parse ballot number from '{}'", number);
            return null;
        }
    }

    private String buildSourceUrl(String href) {
        return UriComponentsBuilder.fromUriString(properties.getBaseUrl())
                .path(href)
                .build()
                .encode()
                .toUri()
                .toString();
    }
}
