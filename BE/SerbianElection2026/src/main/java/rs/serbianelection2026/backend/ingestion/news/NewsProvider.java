package rs.serbianelection2026.backend.ingestion.news;

import java.util.List;
import rs.serbianelection2026.backend.ingestion.entity.ImportSource;
import rs.serbianelection2026.backend.ingestion.news.dto.NormalizedNewsArticle;

/** One implementation per news source (N1, Nova, Blic, Informer); {@link NewsImportService} runs all of them. */
public interface NewsProvider {

    ImportSource getSource();

    List<NormalizedNewsArticle> fetch();
}
