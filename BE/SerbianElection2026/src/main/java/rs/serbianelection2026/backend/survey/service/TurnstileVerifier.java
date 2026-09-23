package rs.serbianelection2026.backend.survey.service;

/** Checks the token the browser got from the Cloudflare Turnstile widget. */
public interface TurnstileVerifier {

    /** False when the check is switched off (no secret configured). */
    boolean isEnabled();

    boolean verify(String token);

    static TurnstileVerifier disabled() {
        return new TurnstileVerifier() {
            @Override
            public boolean isEnabled() {
                return false;
            }

            @Override
            public boolean verify(String token) {
                return true;
            }
        };
    }
}
