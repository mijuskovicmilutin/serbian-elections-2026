package rs.serbianelection2026.backend.poll.repository;

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

public interface PollRepository extends JpaRepository<Poll, Long> {

    @EntityGraph(attributePaths = "pollster")
    Page<Poll> findByStatusOrderByPublishedAtDesc(PollStatus status, Pageable pageable);

    @EntityGraph(attributePaths = "pollster")
    Page<Poll> findByStatusAndPollster_SlugOrderByPublishedAtDesc(PollStatus status, String slug, Pageable pageable);

    @EntityGraph(attributePaths = "pollster")
    Optional<Poll> findByIdAndStatus(Long id, PollStatus status);

    @Query("select p.pollster.id as pollsterId, count(p) as pollCount from Poll p "
            + "where p.status = :status group by p.pollster.id")
    List<PollsterPollCount> countByPollsterForStatus(@Param("status") PollStatus status);

    interface PollsterPollCount {
        Long getPollsterId();

        long getPollCount();
    }
}
