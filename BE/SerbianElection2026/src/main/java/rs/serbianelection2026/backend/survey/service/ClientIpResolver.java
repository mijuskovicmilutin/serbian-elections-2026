package rs.serbianelection2026.backend.survey.service;

import jakarta.servlet.http.HttpServletRequest;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Finds the address of the visitor. Behind a proxy the socket only shows the proxy, so the configured header is
 * used, but only when {@code survey.trust-forwarded-header} is on (otherwise a visitor could invent addresses).
 * The value is used for the duplicate guard and the in-memory rate limit and is never logged or stored.
 */
@Component
public class ClientIpResolver {

    private static final Pattern IPV4 = Pattern.compile("^(\\d{1,3})(\\.\\d{1,3}){3}$");
    private static final Pattern IPV6 = Pattern.compile("^[0-9a-fA-F:.]+(%[0-9A-Za-z._-]+)?$");

    private final SurveyProperties properties;

    public ClientIpResolver(SurveyProperties properties) {
        this.properties = properties;
    }

    public String resolve(HttpServletRequest request) {
        if (properties.isTrustForwardedHeader()) {
            String header = request.getHeader(properties.getForwardedHeader());
            if (header != null && !header.isBlank()) {
                String first = header.split(",")[0].trim();
                if (looksLikeIpLiteral(first)) {
                    return first;
                }
            }
        }
        return request.getRemoteAddr();
    }

    static boolean looksLikeIpLiteral(String value) {
        return IPV4.matcher(value).matches() || (value.contains(":") && IPV6.matcher(value).matches());
    }
}
