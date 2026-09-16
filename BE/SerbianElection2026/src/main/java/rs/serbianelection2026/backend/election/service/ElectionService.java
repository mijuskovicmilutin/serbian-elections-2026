package rs.serbianelection2026.backend.election.service;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.serbianelection2026.backend.common.exception.NotFoundException;
import rs.serbianelection2026.backend.election.entity.Election;
import rs.serbianelection2026.backend.election.entity.ElectoralList;
import rs.serbianelection2026.backend.election.repository.ElectionRepository;
import rs.serbianelection2026.backend.election.repository.ElectoralListRepository;

@Slf4j
@Service
public class ElectionService {

    private final ElectionRepository electionRepository;
    private final ElectoralListRepository electoralListRepository;

    public ElectionService(ElectionRepository electionRepository, ElectoralListRepository electoralListRepository) {
        this.electionRepository = electionRepository;
        this.electoralListRepository = electoralListRepository;
    }

    @Transactional(readOnly = true)
    public Election getCurrentElection() {
        log.info("Fetching current election");

        Election election = electionRepository.findFirstByOrderByElectionDateAsc()
                .orElseThrow(() -> {
                    log.warn("No election found in the database");
                    return new NotFoundException("No election configured");
                });

        log.info("Successfully fetched current election: id={}, name={}", election.getId(), election.getName());
        return election;
    }

    @Transactional(readOnly = true)
    public List<ElectoralList> getCurrentElectoralLists() {
        log.info("Fetching electoral lists for the current election");

        Election election = getCurrentElection();
        List<ElectoralList> lists = electoralListRepository.findByElection_IdOrderByBallotNumberAsc(election.getId());

        log.info("Successfully fetched {} electoral list(s) for election id={}", lists.size(), election.getId());
        return lists;
    }
}
