package rs.serbianelection2026.backend.ingestion.news;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.springframework.stereotype.Component;
import rs.serbianelection2026.backend.ingestion.news.dto.RssItem;

/**
 * Parses a standard RSS 2.0 XML document into {@link RssItem}s. Image extraction tries, in
 * order, a {@code media:content} element, an {@code enclosure} element, then falls back to the
 * first {@code <img>} inside {@code content:encoded} — the three ways N1/Nova, Informer, and
 * Blic respectively embed an article's lead image.
 */
@Slf4j
@Component
public class RssFeedParser {

    public List<RssItem> parse(String xml) {
        Document doc = Jsoup.parse(xml, "", Parser.xmlParser());
        List<Element> items = doc.select("item");
        log.debug("Found {} <item> element(s) in RSS feed", items.size());

        List<RssItem> result = new ArrayList<>();
        int skipped = 0;
        for (Element item : items) {
            try {
                result.add(parseOne(item));
            } catch (RuntimeException e) {
                skipped++;
                log.warn("Skipping unparsable RSS item: {}", item, e);
            }
        }

        log.debug("Parsed {} item(s) successfully, skipped {} unparsable item(s)", result.size(), skipped);
        return result;
    }

    private RssItem parseOne(Element item) {
        String title = text(item, "title");
        String link = text(item, "link");
        if (link == null || link.isBlank()) {
            throw new IllegalArgumentException("Missing <link> in RSS item");
        }
        String guid = text(item, "guid");
        String description = text(item, "description");
        Instant publishedAt = parsePubDate(text(item, "pubDate"));
        String imageUrl = extractImageUrl(item);
        List<String> categories = item.select("category").eachText();

        return new RssItem(title, link, guid, description, publishedAt, imageUrl, categories);
    }

    private String text(Element item, String tagName) {
        Element el = item.selectFirst(tagName);
        return el == null ? null : el.text().trim();
    }

    private Instant parsePubDate(String pubDate) {
        if (pubDate == null || pubDate.isBlank()) {
            throw new IllegalArgumentException("Missing <pubDate> in RSS item");
        }
        return Instant.from(DateTimeFormatter.RFC_1123_DATE_TIME.parse(pubDate.trim()));
    }

    private String extractImageUrl(Element item) {
        // media:content / content:encoded are namespaced tag names (contain a colon), which CSS
        // selectors can't address reliably — getElementsByTag() matches the literal tag name instead.
        Element media = item.getElementsByTag("media:content").first();
        if (media != null && !media.attr("url").isBlank()) {
            return media.attr("url");
        }

        Element enclosure = item.selectFirst("enclosure");
        if (enclosure != null && !enclosure.attr("url").isBlank()) {
            return enclosure.attr("url");
        }

        Element encoded = item.getElementsByTag("content:encoded").first();
        if (encoded != null) {
            Element img = Jsoup.parseBodyFragment(encoded.text()).selectFirst("img");
            if (img != null && !img.attr("src").isBlank()) {
                return img.attr("src");
            }
        }

        return null;
    }
}
