package rs.serbianelection2026.backend.poll.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import rs.serbianelection2026.backend.poll.entity.Poll;
import rs.serbianelection2026.backend.poll.entity.PollStatus;
import rs.serbianelection2026.backend.poll.entity.SourceKind;

public interface PollRepository extends JpaRepository<Poll, Long> {

    @EntityGraph(attributePaths = "pollster")
    Page<Poll> findByStatusOrderByPublishedAtDesc(PollStatus status, Pageable pageable);

    @EntityGraph(attributePaths = "pollster")
    Page<Poll> findByStatusAndPollster_SlugOrderByPublishedAtDesc(PollStatus status, String slug, Pageable pageable);

    @EntityGraph(attributePaths = "pollster")
    Page<Poll> findByStatusInOrderByPublishedAtDesc(Collection<PollStatus> statuses, Pageable pageable);

    @EntityGraph(attributePaths = "pollster")
    Optional<Poll> findWithPollsterById(Long id);

    @EntityGraph(attributePaths = "pollster")
    Optional<Poll> findByIdAndStatus(Long id, PollStatus status);

    List<Poll> findByPollster_IdAndSourceKindAndStatusAndPublishedAtBetween(
            Long pollsterId, SourceKind sourceKind, PollStatus status, Instant from, Instant to);

    boolean existsByPollster_IdAndSourceUrl(Long pollsterId, String sourceUrl);

    boolean existsByPollster_IdAndSourceUrlAndIdNot(Long pollsterId, String sourceUrl, Long id);

    @Query("select p.pollster.id as pollsterId, count(p) as pollCount from Poll p "
            + "where p.status = :status group by p.pollster.id")
    List<PollsterPollCount> countByPollsterForStatus(@Param("status") PollStatus status);

    interface PollsterPollCount {
        Long getPollsterId();

        long getPollCount();
    }
}
