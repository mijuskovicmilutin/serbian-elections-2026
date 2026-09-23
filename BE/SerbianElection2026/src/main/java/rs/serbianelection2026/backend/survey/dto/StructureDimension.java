package rs.serbianelection2026.backend.survey.dto;

import java.util.List;

/** Sample against population for one weighting dimension (why the recalculation is needed). */
public record StructureDimension(String dimension, List<StructureCategory> categories) {
}
