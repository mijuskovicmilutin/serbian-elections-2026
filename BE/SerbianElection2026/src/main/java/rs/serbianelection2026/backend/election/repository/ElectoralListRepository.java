package rs.serbianelection2026.backend.election.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import rs.serbianelection2026.backend.election.entity.ElectoralList;

public interface ElectoralListRepository extends JpaRepository<ElectoralList, Long> {

    Optional<ElectoralList> findByExternalId(String externalId);

    List<ElectoralList> findByElection_IdOrderByBallotNumberAsc(Long electionId);
}
