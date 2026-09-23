package rs.serbianelection2026.backend.survey.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** Server-side check of a Turnstile token. The visitor's address is deliberately not sent to Cloudflare. */
@Slf4j
public class CloudflareTurnstileVerifier implements TurnstileVerifier {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String secret;
    private final String verifyUrl;

    public CloudflareTurnstileVerifier(RestClient restClient, ObjectMapper objectMapper, String secret, String verifyUrl) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.secret = secret;
        this.verifyUrl = verifyUrl;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public boolean verify(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        try {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("secret", secret);
            form.add("response", token);
            String body = restClient.post()
                    .uri(verifyUrl)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(String.class);
            JsonNode json = objectMapper.readTree(body);
            return json.path("success").asBoolean(false);
        } catch (Exception e) {
            // Fail closed: if Cloudflare cannot be reached we do not accept unverified answers.
            log.error("Turnstile verification failed with an error: {}", e.toString());
            return false;
        }
    }
}
