package rs.serbianelection2026.backend.ingestion.rik;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.serbianelection2026.backend.election.entity.Election;
import rs.serbianelection2026.backend.election.entity.ElectoralList;
import rs.serbianelection2026.backend.election.entity.ElectoralListStatus;
import rs.serbianelection2026.backend.election.repository.ElectionRepository;
import rs.serbianelection2026.backend.election.repository.ElectoralListRepository;
import rs.serbianelection2026.backend.ingestion.entity.DataImport;
import rs.serbianelection2026.backend.ingestion.entity.ImportSource;
import rs.serbianelection2026.backend.ingestion.entity.ImportStatus;
import rs.serbianelection2026.backend.ingestion.repository.DataImportRepository;
import rs.serbianelection2026.backend.ingestion.rik.dto.NormalizedElectoralList;
import rs.serbianelection2026.backend.ingestion.rik.dto.RikDocumentRecord;

@Slf4j
@Service
public class RikImportService {

    private final RikClient rikClient;
    private final RikParser rikParser;
    private final ElectionRepository electionRepository;
    private final ElectoralListRepository electoralListRepository;
    private final DataImportRepository dataImportRepository;

    public RikImportService(
            RikClient rikClient,
            RikParser rikParser,
            ElectionRepository electionRepository,
            ElectoralListRepository electoralListRepository,
            DataImportRepository dataImportRepository) {
        this.rikClient = rikClient;
        this.rikParser = rikParser;
        this.electionRepository = electionRepository;
        this.electoralListRepository = electoralListRepository;
        this.dataImportRepository = dataImportRepository;
    }

    @Transactional
    public DataImport importElectoralLists() {
        log.info("Starting RIK electoral list import");
        Instant start = Instant.now();

        DataImport dataImport = DataImport.builder()
                .source(ImportSource.RIK)
                .startedAt(start)
                .status(ImportStatus.RUNNING)
                .build();
        dataImport = dataImportRepository.save(dataImport);
        log.debug("Created data_import audit row id={} with status=RUNNING", dataImport.getId());

        try {
            Election election = electionRepository.findFirstByOrderByElectionDateAsc()
                    .orElseThrow(() -> new IllegalStateException("No election found to attach electoral lists to"));
            log.debug("Resolved target election: id={}, name={}", election.getId(), election.getName());

            List<RikDocumentRecord> raw = rikClient.fetchElectoralLists();
            List<NormalizedElectoralList> normalized = rikParser.parse(raw);

            log.debug("Upserting {} normalized electoral list(s) into the database", normalized.size());
            int created = 0;
            int updated = 0;
            int unchanged = 0;
            Instant now = Instant.now();

            for (NormalizedElectoralList item : normalized) {
                ElectoralList list = electoralListRepository.findByExternalId(item.externalId()).orElse(null);
                if (list == null) {
                    list = ElectoralList.builder()
                            .election(election)
                            .externalId(item.externalId())
                            .status(ElectoralListStatus.SUBMITTED)
                            .build();
                    applyContent(list, item);
                    created++;
                    log.debug("Creating new electoral list: externalId={}, name={}", item.externalId(), item.name());
                } else if (hasContentChanged(list, item)) {
                    applyContent(list, item);
                    updated++;
                    log.debug("Updating changed electoral list: externalId={}, name={}", item.externalId(), item.name());
                } else {
                    unchanged++;
                    log.debug("No content change for electoral list: externalId={}", item.externalId());
                }
                // Always touch lastSeenAt so we can later tell whether a list has
                // disappeared from RIK's feed, even on runs where nothing else changed.
                list.setLastSeenAt(now);
                electoralListRepository.save(list);
            }

            dataImport.setFinishedAt(Instant.now());
            dataImport.setStatus(ImportStatus.SUCCESS);
            dataImport.setRecordsFound(normalized.size());
            dataImport.setRecordsCreated(created);
            dataImport.setRecordsUpdated(updated);
            dataImport.setRecordsUnchanged(unchanged);

            log.info(
                    "RIK electoral list import succeeded in {} ms: found={}, created={}, updated={}, unchanged={}",
                    Duration.between(start, Instant.now()).toMillis(),
                    normalized.size(),
                    created,
                    updated,
                    unchanged);
        } catch (Exception e) {
            dataImport.setFinishedAt(Instant.now());
            dataImport.setStatus(ImportStatus.FAILED);
            dataImport.setErrorMessage(e.getMessage());
            log.error(
                    "RIK electoral list import failed after {} ms",
                    Duration.between(start, Instant.now()).toMillis(),
                    e);
        }

        DataImport saved = dataImportRepository.save(dataImport);
        log.debug("Saved data_import audit row id={} with final status={}", saved.getId(), saved.getStatus());
        return saved;
    }

    private boolean hasContentChanged(ElectoralList list, NormalizedElectoralList item) {
        return !Objects.equals(list.getName(), item.name())
                || !Objects.equals(list.getBallotNumber(), item.ballotNumber())
                || !Objects.equals(list.getSourceUrl(), item.sourceUrl())
                || !Objects.equals(list.getPublishedAt(), item.publishedAt());
    }

    private void applyContent(ElectoralList list, NormalizedElectoralList item) {
        list.setName(item.name());
        list.setBallotNumber(item.ballotNumber());
        list.setSourceUrl(item.sourceUrl());
        list.setPublishedAt(item.publishedAt());
    }
}
