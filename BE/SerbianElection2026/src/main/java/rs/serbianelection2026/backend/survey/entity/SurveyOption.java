package rs.serbianelection2026.backend.survey.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "survey_option")
public class SurveyOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "question_id", nullable = false)
    private Long questionId;

    @Column(nullable = false)
    private int position;

    /** Stable key, e.g. {@code AGE_18_29}; population margins are matched on it. */
    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 500)
    private String label;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SurveyOptionKind kind;

    /** Set for the options that are electoral lists. */
    @Column(name = "electoral_list_id")
    private Long electoralListId;

    /** False once RIK rejects or withdraws the list; such an option is neither offered nor counted. */
    @Column(nullable = false)
    private boolean active;

    /** Only set when the option was added after answers had already been collected. */
    @Column(name = "added_on")
    private LocalDate addedOn;
}
