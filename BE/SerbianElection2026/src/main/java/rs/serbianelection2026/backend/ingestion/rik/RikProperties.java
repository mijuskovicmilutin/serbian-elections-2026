package rs.serbianelection2026.backend.ingestion.rik;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "rik")
public class RikProperties {

    private String baseUrl;
    private String electionRoundId;
    private String electoralListDocumentType;
}
