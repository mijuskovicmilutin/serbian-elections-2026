package rs.serbianelection2026.backend.news.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.serbianelection2026.backend.ingestion.entity.ImportSource;
import rs.serbianelection2026.backend.news.entity.NewsArticle;
import rs.serbianelection2026.backend.news.repository.NewsArticleRepository;

@Slf4j
@Service
public class NewsService {

    private final NewsArticleRepository newsArticleRepository;

    public NewsService(NewsArticleRepository newsArticleRepository) {
        this.newsArticleRepository = newsArticleRepository;
    }

    @Transactional(readOnly = true)
    public Page<NewsArticle> getNews(ImportSource source, Pageable pageable) {
        log.info("Fetching news: source={}, page={}, size={}", source, pageable.getPageNumber(), pageable.getPageSize());

        Page<NewsArticle> result = source == null
                ? newsArticleRepository.findAllByOrderByPublishedAtDesc(pageable)
                : newsArticleRepository.findBySourceOrderByPublishedAtDesc(source, pageable);

        log.info(
                "Successfully fetched {} news article(s) (page {} of {})",
                result.getNumberOfElements(),
                result.getNumber(),
                result.getTotalPages());
        return result;
    }
}
