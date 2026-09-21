package rs.serbianelection2026.backend.ingestion.rik;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

    /** The list number RIK assigned is the prefix of the document title, e.g. "10. ИЗБОРНА ЛИСТА ..." or "8.ИЗБОРНА ЛИСТА ...". */
    private static final Pattern TITLE_NUMBER = Pattern.compile("^\\s*(\\d{1,3})\\s*\\.");

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
        String name = record.documentName() == null ? null : record.documentName().trim();
        Integer ballotNumber = parseBallotNumber(name);
        Instant publishedAt = Instant.ofEpochSecond(Long.parseLong(record.datetime()));
        String sourceUrl = buildSourceUrl(href);
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

    /**
     * The number comes from the title, not from the "number" column of the RIK response: that column is
     * just the row position in whatever order the response happens to be in, so it shifts as new lists
     * are published and would put lists in the wrong order.
     */
    private Integer parseBallotNumber(String name) {
        if (name == null) {
            return null;
        }
        Matcher matcher = TITLE_NUMBER.matcher(name);
        if (!matcher.find()) {
            log.warn("No list number at the start of the RIK document title: '{}'", name);
            return null;
        }
        return Integer.valueOf(matcher.group(1));
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
