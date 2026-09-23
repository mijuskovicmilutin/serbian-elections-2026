package rs.serbianelection2026.backend.survey.service;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "survey")
public class SurveyProperties {

    /**
     * Secret for the keyed hash of a network (see {@link NetworkHasher}). Blank disables answer submission, so a
     * forgotten variable can never make the survey accept answers without its duplicate guard.
     */
    private String hashKey = "";

    /** Cloudflare Turnstile secret. Blank switches the bot check off (local development only). */
    private String turnstileSecret = "";

    private String turnstileVerifyUrl = "https://challenges.cloudflare.com/turnstile/v0/siteverify";

    /** Read the client address from {@link #forwardedHeader} instead of the socket (only behind a trusted proxy). */
    private boolean trustForwardedHeader = false;

    private String forwardedHeader = "X-Forwarded-For";

    /** Answers one address may send per hour, counted in memory only. */
    private int rateLimitPerHour = 20;
}
