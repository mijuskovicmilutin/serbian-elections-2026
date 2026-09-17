package rs.serbianelection2026.backend.ingestion.news;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import rs.serbianelection2026.backend.ingestion.news.dto.RssItem;

class RssFeedParserTest {

    private final RssFeedParser parser = new RssFeedParser();

    @Test
    void parsesN1FeedExtractingMediaContentImageAndCategories() throws IOException {
        List<RssItem> items = parser.parse(loadFixture("n1-feed.xml"));

        assertThat(items).hasSize(3);

        RssItem first = items.get(0);
        assertThat(first.title()).isEqualTo(
                "RIK proglasio liste Usame Zukorlić Pokret za ujedinjenje i Ruska stranka - Srbija u Briksu");
        assertThat(first.link()).isEqualTo(
                "https://n1info.rs/vesti/rik-proglasio-liste-usame-zukorlic-pokret-za-ujedinjenje-i-ruska-stranka-srbija-u-briksu/");
        assertThat(first.guid()).isEqualTo(first.link());
        assertThat(first.imageUrl()).isEqualTo(
                "https://n1info.rs/media/images/2026/9/11/1789137028__1700089_b.width-1200.JPG");
        assertThat(first.categories()).contains("Vesti", "usame zukorlić", "izbori 2026");
        assertThat(first.publishedAt()).isEqualTo(Instant.parse("2026-09-17T20:19:37Z"));

        RssItem sportItem = items.get(2);
        assertThat(sportItem.categories()).containsExactly("Ostali sportovi", "odbojkaši", "evropsko prvenstvo");
    }

    @Test
    void parsesBlicFeedFallingBackToImgInsideContentEncoded() throws IOException {
        List<RssItem> items = parser.parse(loadFixture("blic-feed.xml"));

        assertThat(items).hasSize(2);
        RssItem first = items.get(0);
        assertThat(first.imageUrl()).startsWith("https://ocdn.eu/pulscms-transforms/");
        assertThat(first.guid()).isEqualTo(first.link());
        assertThat(first.categories()).isEmpty();
    }

    @Test
    void parsesInformerFeedUsingEnclosureForImage() throws IOException {
        List<RssItem> items = parser.parse(loadFixture("informer-feed.xml"));

        assertThat(items).hasSize(2);
        RssItem first = items.get(0);
        assertThat(first.imageUrl()).isEqualTo("https://informer.rs/data/images/2026-09-17/1432252_politika-2_ig.jpg");
        assertThat(first.categories()).containsExactly("Politika");
    }

    @Test
    void parsesNovaFeedAlreadyScopedToPolitics() throws IOException {
        List<RssItem> items = parser.parse(loadFixture("nova-feed.xml"));

        assertThat(items).hasSize(2);
        assertThat(items).allSatisfy(item -> assertThat(item.categories()).contains("Politika"));
    }

    private String loadFixture(String name) throws IOException {
        try (InputStream in = getClass().getResourceAsStream("/news/" + name)) {
            if (in == null) {
                throw new IllegalStateException("Fixture not found on classpath: " + name);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
