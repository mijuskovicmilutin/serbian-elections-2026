package rs.serbianelection2026.backend.survey.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import rs.serbianelection2026.backend.survey.entity.PopulationMargin;

public interface PopulationMarginRepository extends JpaRepository<PopulationMargin, Long> {

    List<PopulationMargin> findByDimension(String dimension);
}
