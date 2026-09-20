package rs.serbianelection2026.backend.poll.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import rs.serbianelection2026.backend.poll.entity.Pollster;

public interface PollsterRepository extends JpaRepository<Pollster, Long> {

    Optional<Pollster> findBySlug(String slug);

    boolean existsBySlugAndActiveTrue(String slug);

    List<Pollster> findByActiveTrueOrderByNameAsc();
}
