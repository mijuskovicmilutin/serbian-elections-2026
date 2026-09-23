package rs.serbianelection2026.backend.survey.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Embeddable
public class SurveyAnswerId implements Serializable {

    @Column(name = "response_id")
    private Long responseId;

    @Column(name = "question_id")
    private Long questionId;
}
