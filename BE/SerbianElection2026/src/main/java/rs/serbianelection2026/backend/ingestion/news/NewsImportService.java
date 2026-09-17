package rs.serbianelection2026.backend.ingestion.news;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.serbianelection2026.backend.ingestion.entity.DataImport;
import rs.serbianelection2026.backend.ingestion.entity.ImportSource;
import rs.serbianelection2026.backend.ingestion.entity.ImportStatus;
import rs.serbianelection2026.backend.ingestion.news.dto.NormalizedNewsArticle;
import rs.serbianelection2026.backend.ingestion.repository.DataImportRepository;
import rs.serbianelection2026.backend.news.entity.NewsArticle;
import rs.serbianelection2026.backend.news.repository.NewsArticleRepository;

@Slf4j
@Service
public class NewsImportService {

    private final NewsArticleRepository newsArticleRepository;
    private final DataImportRepository dataImportRepository;

    public NewsImportService(NewsArticleRepository newsArticleRepository, DataImportRepository dataImportRepository) {
        this.newsArticleRepository = newsArticleRepository;
        this.dataImportRepository = dataImportRepository;
    }

    @Transactional
    public DataImport importSource(NewsProvider provider) {
        ImportSource source = provider.getSource();
        log.info("Starting {} news import", source);
        Instant start = Instant.now();

        DataImport dataImport = DataImport.builder()
                .source(source)
                .startedAt(start)
                .status(ImportStatus.RUNNING)
                .build();
        dataImport = dataImportRepository.save(dataImport);
        log.debug("Created data_import audit row id={} for source={} with status=RUNNING", dataImport.getId(), source);

        try {
            List<NormalizedNewsArticle> fetched = provider.fetch();

            int created = 0;
            int alreadyKnown = 0;
            for (NormalizedNewsArticle item : fetched) {
                if (newsArticleRepository.existsBySourceAndUrl(source, item.url())) {
                    alreadyKnown++;
                    continue;
                }
                NewsArticle article = NewsArticle.builder()
                        .source(source)
                        .externalId(item.externalId())
                        .title(item.title())
                        .description(item.description())
                        .url(item.url())
                        .imageUrl(item.imageUrl())
                        .publishedAt(item.publishedAt())
                        .fetchedAt(Instant.now())
                        .build();
                newsArticleRepository.save(article);
                created++;
                log.debug("Saved new {} article: {}", source, item.url());
            }

            dataImport.setFinishedAt(Instant.now());
            dataImport.setStatus(ImportStatus.SUCCESS);
            dataImport.setRecordsFound(fetched.size());
            dataImport.setRecordsCreated(created);
            dataImport.setRecordsUnchanged(alreadyKnown);

            log.info(
                    "{} news import succeeded in {} ms: found={}, created={}, alreadyKnown={}",
                    source,
                    Duration.between(start, Instant.now()).toMillis(),
                    fetched.size(),
                    created,
                    alreadyKnown);
        } catch (Exception e) {
            dataImport.setFinishedAt(Instant.now());
            dataImport.setStatus(ImportStatus.FAILED);
            dataImport.setErrorMessage(e.getMessage());
            log.error(
                    "{} news import failed after {} ms",
                    source,
                    Duration.between(start, Instant.now()).toMillis(),
                    e);
        }

        DataImport saved = dataImportRepository.save(dataImport);
        log.debug("Saved data_import audit row id={} with final status={}", saved.getId(), saved.getStatus());
        return saved;
    }
}
