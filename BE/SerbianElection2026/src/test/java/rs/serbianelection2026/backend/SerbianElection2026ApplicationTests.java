package rs.serbianelection2026.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(
        properties = {"rik.import-enabled=false", "news.import-enabled=false", "polymarket.import-enabled=false",
            "polls.discovery.enabled=false"})
class SerbianElection2026ApplicationTests {

    @Test
    void contextLoads() {
    }

}
