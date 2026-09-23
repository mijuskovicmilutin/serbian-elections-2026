package rs.serbianelection2026.backend.survey.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import rs.serbianelection2026.backend.common.exception.ServiceUnavailableException;

class NetworkHasherTest {

    private static NetworkHasher hasher(String key) {
        SurveyProperties properties = new SurveyProperties();
        properties.setHashKey(key);
        return new NetworkHasher(properties);
    }

    @Test
    void sameNetworkGivesTheSameHashAndItIsAKeyedDigest() {
        NetworkHasher hasher = hasher("secret");

        String a = hasher.hash(1, "203.0.113.7");
        assertThat(hasher.hash(1, "203.0.113.7")).isEqualTo(a);
        assertThat(a).matches("[0-9a-f]{64}").doesNotContain("203");
    }

    @Test
    void hashDiffersPerSurveyAndPerKey() {
        assertThat(hasher("secret").hash(1, "203.0.113.7")).isNotEqualTo(hasher("secret").hash(2, "203.0.113.7"));
        assertThat(hasher("secret").hash(1, "203.0.113.7")).isNotEqualTo(hasher("other").hash(1, "203.0.113.7"));
        assertThat(hasher("secret").hash(1, "203.0.113.7")).isNotEqualTo(hasher("secret").hash(1, "203.0.113.8"));
    }

    @Test
    void anIpv6AddressIsReducedToItsSlash64Prefix() {
        NetworkHasher hasher = hasher("secret");

        assertThat(hasher.hash(1, "2001:db8:abcd:12:1:2:3:4")).isEqualTo(hasher.hash(1, "2001:db8:abcd:12:ffff:ffff:ffff:ffff"));
        assertThat(hasher.hash(1, "2001:db8:abcd:12::1")).isNotEqualTo(hasher.hash(1, "2001:db8:abcd:13::1"));
    }

    @Test
    void anIpv4MappedIpv6AddressCountsAsTheIpv4Address() {
        NetworkHasher hasher = hasher("secret");

        assertThat(hasher.hash(1, "::ffff:203.0.113.7")).isEqualTo(hasher.hash(1, "203.0.113.7"));
    }

    @Test
    void networkNormalisation() {
        assertThat(NetworkHasher.network(" 10.0.0.1 ")).isEqualTo("10.0.0.1");
        assertThat(NetworkHasher.network("2001:db8:abcd:12:1:2:3:4")).isEqualTo("v6:20010db8abcd0012");
        assertThat(NetworkHasher.network("not-an-address")).isEqualTo("unrecognised");
        assertThat(NetworkHasher.network(null)).isEqualTo("unrecognised");
    }

    @Test
    void withoutAKeyNothingIsHashedAndSubmissionIsDisabled() {
        NetworkHasher hasher = hasher("");

        assertThat(hasher.isConfigured()).isFalse();
        assertThatThrownBy(() -> hasher.hash(1, "10.0.0.1")).isInstanceOf(ServiceUnavailableException.class);
    }
}
