package rs.serbianelection2026.backend.ingestion.rik;

import java.time.Instant;
import java.util.List;
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
        DataImport dataImport = DataImport.builder()
                .source(ImportSource.RIK)
                .startedAt(Instant.now())
                .status(ImportStatus.RUNNING)
                .build();
        dataImport = dataImportRepository.save(dataImport);

        try {
            Election election = electionRepository.findFirstByOrderByElectionDateAsc()
                    .orElseThrow(() -> new IllegalStateException("No election found to attach electoral lists to"));

            List<RikDocumentRecord> raw = rikClient.fetchElectoralLists();
            List<NormalizedElectoralList> normalized = rikParser.parse(raw);

            int created = 0;
            int updated = 0;
            Instant now = Instant.now();

            for (NormalizedElectoralList item : normalized) {
                ElectoralList list = electoralListRepository.findByExternalId(item.externalId()).orElse(null);
                if (list == null) {
                    list = ElectoralList.builder()
                            .election(election)
                            .externalId(item.externalId())
                            .status(ElectoralListStatus.SUBMITTED)
                            .build();
                    created++;
                } else {
                    updated++;
                }
                list.setName(item.name());
                list.setBallotNumber(item.ballotNumber());
                list.setSourceUrl(item.sourceUrl());
                list.setPublishedAt(item.publishedAt());
                list.setLastSeenAt(now);
                electoralListRepository.save(list);
            }

            dataImport.setFinishedAt(Instant.now());
            dataImport.setStatus(ImportStatus.SUCCESS);
            dataImport.setRecordsFound(normalized.size());
            dataImport.setRecordsCreated(created);
            dataImport.setRecordsUpdated(updated);
        } catch (Exception e) {
            log.error("RIK electoral list import failed", e);
            dataImport.setFinishedAt(Instant.now());
            dataImport.setStatus(ImportStatus.FAILED);
            dataImport.setErrorMessage(e.getMessage());
        }

        return dataImportRepository.save(dataImport);
    }
}
