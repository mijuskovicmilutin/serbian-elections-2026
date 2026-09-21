package rs.serbianelection2026.backend.ingestion.poll;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;
import rs.serbianelection2026.backend.ingestion.entity.DataImport;
import rs.serbianelection2026.backend.ingestion.entity.ImportStatus;
import rs.serbianelection2026.backend.ingestion.poll.dto.DiscoveredItem;
import rs.serbianelection2026.backend.ingestion.repository.DataImportRepository;
import rs.serbianelection2026.backend.poll.dto.MediaSourceInput;
import rs.serbianelection2026.backend.poll.entity.Poll;
import rs.serbianelection2026.backend.poll.entity.PollStatus;
import rs.serbianelection2026.backend.poll.entity.Pollster;
import rs.serbianelection2026.backend.poll.entity.SourceKind;
import rs.serbianelection2026.backend.poll.repository.PollRepository;
import rs.serbianelection2026.backend.poll.repository.PollsterRepository;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * Turns feed items that look like a poll into {@code DISCOVERED} candidates (title, link, date and the
 * text we saw). It never extracts numbers: what a poll's percentages mean is decided by a human in review.
 */
@Slf4j
@Service
public class PollDiscoveryService {

    /** Media reports of the same poll land within days of each other; they are merged into one candidate. */
    private static final Duration SAME_POLL_WINDOW = Duration.ofDays(3);

    private static final int SNAPSHOT_LIMIT = 4000;
    private static final int TITLE_LIMIT = 500;

    private final PollRepository pollRepository;
    private final PollsterRepository pollsterRepository;
    private final DataImportRepository dataImportRepository;
    private final PollTextMatcher matcher;
    private final PollDiscoveryProperties properties;
    private final ObjectMapper objectMapper;

    public PollDiscoveryService(
            PollRepository pollRepository,
            PollsterRepository pollsterRepository,
            DataImportRepository dataImportRepository,
            PollTextMatcher matcher,
            PollDiscoveryProperties properties,
            ObjectMapper objectMapper) {
        this.pollRepository = pollRepository;
        this.pollsterRepository = pollsterRepository;
        this.dataImportRepository = dataImportRepository;
        this.matcher = matcher;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public DataImport discover(PollDiscoveryFeed feed) {
        log.info("Starting poll discovery: source={}", feed.source());
        Instant start = Instant.now();

        DataImport dataImport = dataImportRepository.save(DataImport.builder()
                .source(feed.source())
                .startedAt(start)
                .status(ImportStatus.RUNNING)
                .build());

        try {
            List<DiscoveredItem> items = feed.fetch();
            List<Pollster> pollsters = pollsterRepository.findByActiveTrueOrderByNameAsc();
            Instant oldest = start.minus(Duration.ofDays(properties.getMaxAgeDays()));

            int found = 0;
            int created = 0;
            int merged = 0;
            int known = 0;
            for (DiscoveredItem item : items) {
                Optional<Pollster> pollster = accept(item, pollsters, oldest);
                if (pollster.isEmpty()) {
                    continue;
                }
                found++;
                switch (record(item, pollster.get())) {
                    case CREATED -> created++;
                    case MERGED -> merged++;
                    case KNOWN -> known++;
                }
            }

            dataImport.setFinishedAt(Instant.now());
            dataImport.setStatus(ImportStatus.SUCCESS);
            dataImport.setRecordsFound(found);
            dataImport.setRecordsCreated(created);
            dataImport.setRecordsUpdated(merged);
            dataImport.setRecordsUnchanged(known);
            log.info(
                    "Poll discovery succeeded in {} ms: source={}, scanned={}, poll-like={}, new candidates={}, merged={}, already known={}",
                    Duration.between(start, Instant.now()).toMillis(),
                    feed.source(),
                    items.size(),
                    found,
                    created,
                    merged,
                    known);
        } catch (Exception e) {
            dataImport.setFinishedAt(Instant.now());
            dataImport.setStatus(ImportStatus.FAILED);
            dataImport.setErrorMessage(e.getMessage());
            log.error(
                    "Poll discovery failed after {} ms: source={}", Duration.between(start, Instant.now()).toMillis(), feed.source(), e);
        }
        return dataImportRepository.save(dataImport);
    }

    private enum Outcome {
        CREATED, MERGED, KNOWN
    }

    /** Returns the pollster when the item is recent enough and looks like a poll report. */
    private Optional<Pollster> accept(DiscoveredItem item, List<Pollster> pollsters, Instant oldest) {
        if (item.link() == null || item.link().isBlank() || item.title() == null || item.title().isBlank()) {
            return Optional.empty();
        }
        if (item.publishedAt() == null || item.publishedAt().isBefore(oldest)) {
            return Optional.empty();
        }
        String text = matcher.normalize(item.title() + " " + plainText(item.description()));
        if (!matcher.mentionsPoll(text)) {
            return Optional.empty();
        }
        boolean secondary = item.sourceKind() == SourceKind.SECONDARY;
        if (secondary && !matcher.hasPercentage(text)) {
            return Optional.empty();
        }
        if (item.pollsterSlug() != null) {
            return pollsters.stream().filter(p -> p.getSlug().equals(item.pollsterSlug())).findFirst();
        }
        return matcher.findPollster(text, pollsters);
    }

    private Outcome record(DiscoveredItem item, Pollster pollster) {
        String link = item.link().trim();
        if (pollRepository.existsByPollster_IdAndSourceUrl(pollster.getId(), link)) {
            return Outcome.KNOWN;
        }

        if (item.sourceKind() == SourceKind.SECONDARY) {
            Optional<Poll> sameReport = findUntouchedCandidate(pollster, item.publishedAt());
            if (sameReport.isPresent()) {
                addMediaSource(sameReport.get(), item);
                log.info("Merged media report into candidate id={}: {}", sameReport.get().getId(), link);
                return Outcome.MERGED;
            }
        }

        String description = plainText(item.description());
        String title = item.title().trim();
        String snapshot = title + (description.isEmpty() ? "" : "\n\n" + description);
        pollRepository.save(Poll.builder()
                .pollster(pollster)
                .title(title.length() > TITLE_LIMIT ? title.substring(0, TITLE_LIMIT) : title)
                .publishedAt(item.publishedAt())
                .sourceKind(item.sourceKind())
                .sourceUrl(link)
                .mediaSources(item.mediaName() == null ? null : serialize(List.of(new MediaSourceInput(item.mediaName(), link))))
                .status(PollStatus.DISCOVERED)
                .scrapedAt(Instant.now())
                .contentHash(sha256(title + "\n" + description))
                .sourceSnapshot(snapshot.length() > SNAPSHOT_LIMIT ? snapshot.substring(0, SNAPSHOT_LIMIT) : snapshot)
                .build());
        log.info("New poll candidate: pollster={}, url={}", pollster.getSlug(), link);
        return Outcome.CREATED;
    }

    /** Only a candidate nobody has opened yet is extended, so reviewed data is never changed behind the reviewer's back. */
    private Optional<Poll> findUntouchedCandidate(Pollster pollster, Instant publishedAt) {
        return pollRepository
                .findByPollster_IdAndSourceKindAndStatusAndPublishedAtBetween(
                        pollster.getId(),
                        SourceKind.SECONDARY,
                        PollStatus.DISCOVERED,
                        publishedAt.minus(SAME_POLL_WINDOW),
                        publishedAt.plus(SAME_POLL_WINDOW))
                .stream()
                .findFirst();
    }

    private void addMediaSource(Poll candidate, DiscoveredItem item) {
        List<MediaSourceInput> sources = new ArrayList<>();
        if (candidate.getMediaSources() != null) {
            sources.addAll(objectMapper.readValue(candidate.getMediaSources(), new TypeReference<List<MediaSourceInput>>() {
            }));
        }
        sources.add(new MediaSourceInput(item.mediaName(), item.link().trim()));
        candidate.setMediaSources(serialize(sources));
        pollRepository.save(candidate);
    }

    private String serialize(List<MediaSourceInput> sources) {
        return objectMapper.writeValueAsString(sources);
    }

    private static String plainText(String html) {
        return html == null ? "" : Jsoup.parse(html).text().trim();
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is always available", e);
        }
    }
}
