package rs.serbianelection2026.backend.common.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "admin")
public class AdminProperties {

    /** Shared secret expected in the {@code X-Admin-Key} header. Blank means the internal API is disabled. */
    private String apiKey;
}
