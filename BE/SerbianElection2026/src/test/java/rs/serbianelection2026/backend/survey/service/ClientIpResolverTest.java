package rs.serbianelection2026.backend.survey.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

class ClientIpResolverTest {

    private static ClientIpResolver resolver(boolean trust) {
        SurveyProperties properties = new SurveyProperties();
        properties.setTrustForwardedHeader(trust);
        return new ClientIpResolver(properties);
    }

    private static HttpServletRequest request(String remote, String forwarded) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn(remote);
        when(request.getHeader("X-Forwarded-For")).thenReturn(forwarded);
        return request;
    }

    @Test
    void theSocketAddressIsUsedUnlessTheHeaderIsTrusted() {
        assertThat(resolver(false).resolve(request("10.1.1.1", "198.51.100.9"))).isEqualTo("10.1.1.1");
    }

    @Test
    void aTrustedHeaderUsesItsFirstEntry() {
        assertThat(resolver(true).resolve(request("10.1.1.1", "198.51.100.9, 10.2.2.2"))).isEqualTo("198.51.100.9");
        assertThat(resolver(true).resolve(request("10.1.1.1", "2001:db8::1"))).isEqualTo("2001:db8::1");
    }

    @Test
    void aMissingOrGarbageHeaderFallsBackToTheSocketAddress() {
        assertThat(resolver(true).resolve(request("10.1.1.1", null))).isEqualTo("10.1.1.1");
        assertThat(resolver(true).resolve(request("10.1.1.1", "  "))).isEqualTo("10.1.1.1");
        assertThat(resolver(true).resolve(request("10.1.1.1", "evil.example.com"))).isEqualTo("10.1.1.1");
    }
}
