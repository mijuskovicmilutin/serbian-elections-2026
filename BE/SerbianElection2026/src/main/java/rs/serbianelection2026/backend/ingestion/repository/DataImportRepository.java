package rs.serbianelection2026.backend.ingestion.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import rs.serbianelection2026.backend.ingestion.entity.ImportSource;
import rs.serbianelection2026.backend.ingestion.entity.DataImport;

public interface DataImportRepository extends JpaRepository<DataImport, Long> {

    Optional<DataImport> findFirstBySourceOrderByStartedAtDesc(ImportSource source);
}
