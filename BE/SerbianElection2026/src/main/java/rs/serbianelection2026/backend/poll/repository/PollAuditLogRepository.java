package rs.serbianelection2026.backend.poll.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import rs.serbianelection2026.backend.poll.entity.PollAuditLog;

public interface PollAuditLogRepository extends JpaRepository<PollAuditLog, Long> {

    List<PollAuditLog> findTop50ByPoll_IdOrderByCreatedAtDescIdDesc(Long pollId);
}
