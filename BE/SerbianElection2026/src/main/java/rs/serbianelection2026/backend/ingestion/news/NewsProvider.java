package rs.serbianelection2026.backend.ingestion.news;

import java.util.List;
import rs.serbianelection2026.backend.ingestion.entity.ImportSource;
import rs.serbianelection2026.backend.ingestion.news.dto.NormalizedNewsArticle;
import rs.serbianelection2026.backend.ingestion.news.dto.RssItem;

/** One implementation per news source (N1, Nova, Blic, Informer); {@link NewsImportService} runs all of them. */
public interface NewsProvider {

    ImportSource getSource();

    List<NormalizedNewsArticle> fetch();

    /** Every item currently in the feed (not capped to the latest few), for callers that scan it, e.g. poll discovery. */
    List<RssItem> fetchAllItems();
}
