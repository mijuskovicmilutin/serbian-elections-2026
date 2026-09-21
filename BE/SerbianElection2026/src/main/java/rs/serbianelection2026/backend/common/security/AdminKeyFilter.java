package rs.serbianelection2026.backend.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import rs.serbianelection2026.backend.common.exception.ApiError;
import tools.jackson.databind.ObjectMapper;

/**
 * Guards {@code /internal/**} with a single shared secret (no accounts). With no key configured the
 * internal API behaves as if it did not exist, so a forgotten env var can never leave it open.
 */
@Slf4j
@Component
public class AdminKeyFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Admin-Key";
    private static final String PREFIX = "/internal";

    private final AdminProperties properties;
    private final ObjectMapper objectMapper;

    public AdminKeyFilter(AdminProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !(path.equals(PREFIX) || path.startsWith(PREFIX + "/"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String expected = properties.getApiKey();
        if (expected == null || expected.isBlank()) {
            log.warn("Internal API request rejected because admin.api-key is not configured: {} {}", request.getMethod(), request.getRequestURI());
            reject(response, HttpStatus.NOT_FOUND, "NOT_FOUND", "No such endpoint");
            return;
        }

        String provided = request.getHeader(HEADER);
        if (provided == null || !matches(provided, expected)) {
            log.warn("Internal API request rejected: invalid or missing key: {} {}", request.getMethod(), request.getRequestURI());
            reject(response, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Missing or invalid admin key");
            return;
        }

        chain.doFilter(request, response);
    }

    /** Compares digests so neither the content nor the length of the secret is observable through timing. */
    private boolean matches(String provided, String expected) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            byte[] a = sha.digest(provided.getBytes(StandardCharsets.UTF_8));
            byte[] b = sha.digest(expected.getBytes(StandardCharsets.UTF_8));
            return MessageDigest.isEqual(a, b);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is always available", e);
        }
    }

    private void reject(HttpServletResponse response, HttpStatus status, String code, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(new ApiError(code, message, Instant.now())));
    }
}
