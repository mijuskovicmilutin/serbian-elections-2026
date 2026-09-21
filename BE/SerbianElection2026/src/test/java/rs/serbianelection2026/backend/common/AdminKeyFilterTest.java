package rs.serbianelection2026.backend.common;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import rs.serbianelection2026.backend.common.security.AdminKeyFilter;
import rs.serbianelection2026.backend.common.security.AdminProperties;
import tools.jackson.databind.json.JsonMapper;

class AdminKeyFilterTest {

    private AdminKeyFilter filter(String key) {
        AdminProperties properties = new AdminProperties();
        properties.setApiKey(key);
        return new AdminKeyFilter(properties, JsonMapper.builder().build());
    }

    private MockHttpServletResponse run(AdminKeyFilter filter, String path, String headerValue) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        request.setRequestURI(path);
        if (headerValue != null) {
            request.addHeader(AdminKeyFilter.HEADER, headerValue);
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = new MockFilterChain();
        filter.doFilter(request, response, chain);
        return response;
    }

    private boolean reachedController(MockHttpServletResponse response) {
        return response.getStatus() == 200;
    }

    @Test
    void publicPathsAreNotTouchedEvenWithoutAKey() throws Exception {
        assertThat(reachedController(run(filter("secret"), "/api/v1/polls", null))).isTrue();
        assertThat(reachedController(run(filter(null), "/api/v1/polls", null))).isTrue();
    }

    @Test
    void correctKeyPassesThrough() throws Exception {
        assertThat(reachedController(run(filter("secret"), "/internal/polls", "secret"))).isTrue();
    }

    @Test
    void missingOrWrongKeyIsUnauthorized() throws Exception {
        assertThat(run(filter("secret"), "/internal/polls", null).getStatus()).isEqualTo(401);
        assertThat(run(filter("secret"), "/internal/polls", "wrong").getStatus()).isEqualTo(401);
        assertThat(run(filter("secret"), "/internal/polls", "secre").getStatus()).isEqualTo(401);
        assertThat(run(filter("secret"), "/internal/polls", "secret ").getStatus()).isEqualTo(401);
    }

    @Test
    void unauthorizedResponseIsJsonAndNeverEchoesTheKey() throws Exception {
        MockHttpServletResponse response = run(filter("secret"), "/internal/polls", "wrong-guess");

        assertThat(response.getContentType()).startsWith("application/json");
        assertThat(response.getContentAsString()).contains("UNAUTHORIZED").doesNotContain("wrong-guess").doesNotContain("secret");
    }

    @Test
    void internalApiDoesNotExistWhenNoKeyIsConfigured() throws Exception {
        assertThat(run(filter(null), "/internal/polls", "anything").getStatus()).isEqualTo(404);
        assertThat(run(filter("  "), "/internal/polls", "  ").getStatus()).isEqualTo(404);
        assertThat(run(filter(""), "/internal", null).getStatus()).isEqualTo(404);
    }

    @Test
    void lookalikePathsAreNotTreatedAsInternal() throws Exception {
        assertThat(reachedController(run(filter("secret"), "/internally/x", null))).isTrue();
    }
}
