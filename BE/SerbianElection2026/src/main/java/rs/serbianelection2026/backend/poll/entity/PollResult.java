package rs.serbianelection2026.backend.poll.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.serbianelection2026.backend.common.entity.AuditableEntity;
import rs.serbianelection2026.backend.election.entity.ElectoralList;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "poll_result")
public class PollResult extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "poll_id", nullable = false)
    private Poll poll;

    /** The option name exactly as the source published it; never altered. */
    @Column(name = "raw_option_name", nullable = false, length = 500)
    private String rawOptionName;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal percentage;

    /** Position in the source, so options are shown in the order the pollster published them. */
    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "option_kind", nullable = false, length = 20)
    private OptionKind optionKind;

    @Column(columnDefinition = "TEXT")
    private String composition;

    /** Only set where the link to an electoral list is unambiguous and confirmed in review. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "electoral_list_id")
    private ElectoralList electoralList;
}
