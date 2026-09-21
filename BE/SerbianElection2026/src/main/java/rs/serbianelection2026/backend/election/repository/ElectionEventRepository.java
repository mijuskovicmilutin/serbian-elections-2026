package rs.serbianelection2026.backend.election.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import rs.serbianelection2026.backend.election.entity.ElectionEvent;

public interface ElectionEventRepository extends JpaRepository<ElectionEvent, Long> {

    List<ElectionEvent> findByElection_IdOrderByEventDateAscIdAsc(Long electionId);
}
