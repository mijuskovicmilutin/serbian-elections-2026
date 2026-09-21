package rs.serbianelection2026.backend.common.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AdminProperties.class)
public class AdminConfig {
}
