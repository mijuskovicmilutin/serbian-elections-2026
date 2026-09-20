package rs.serbianelection2026.backend.poll.repository;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import rs.serbianelection2026.backend.poll.entity.PollResult;

public interface PollResultRepository extends JpaRepository<PollResult, Long> {

    List<PollResult> findByPoll_IdOrderByDisplayOrderAsc(Long pollId);

    List<PollResult> findByPoll_IdInOrderByDisplayOrderAsc(Collection<Long> pollIds);
}
