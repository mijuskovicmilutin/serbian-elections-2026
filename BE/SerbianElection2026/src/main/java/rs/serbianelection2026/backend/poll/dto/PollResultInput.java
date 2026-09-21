package rs.serbianelection2026.backend.poll.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import rs.serbianelection2026.backend.poll.entity.OptionKind;

/** One option row as entered in review; the position in the submitted list becomes its display order. */
public record PollResultInput(
        @NotBlank @Size(max = 500) String rawOptionName,
        @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal percentage,
        OptionKind optionKind,
        String composition,
        Long electoralListId) {
}
