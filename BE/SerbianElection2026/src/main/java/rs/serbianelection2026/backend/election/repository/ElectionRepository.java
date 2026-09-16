package rs.serbianelection2026.backend.election.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import rs.serbianelection2026.backend.election.entity.Election;

public interface ElectionRepository extends JpaRepository<Election, Long> {

    Optional<Election> findFirstByOrderByElectionDateAsc();
}
