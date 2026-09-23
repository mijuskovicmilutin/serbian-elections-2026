package rs.serbianelection2026.backend.survey.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Configuration
@EnableConfigurationProperties(SurveyProperties.class)
public class SurveyConfig {

    @Bean
    public TurnstileVerifier turnstileVerifier(
            SurveyProperties properties, RestClient.Builder restClientBuilder, ObjectMapper objectMapper) {
        if (properties.getTurnstileSecret() == null || properties.getTurnstileSecret().isBlank()) {
            log.warn("survey.turnstile-secret is not set: the bot check is OFF (acceptable for local development only)");
            return TurnstileVerifier.disabled();
        }
        return new CloudflareTurnstileVerifier(
                restClientBuilder.build(), objectMapper, properties.getTurnstileSecret(), properties.getTurnstileVerifyUrl());
    }
}
