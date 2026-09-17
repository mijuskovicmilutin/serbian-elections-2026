package rs.serbianelection2026.backend.news.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import rs.serbianelection2026.backend.ingestion.entity.ImportSource;
import rs.serbianelection2026.backend.news.entity.NewsArticle;

public interface NewsArticleRepository extends JpaRepository<NewsArticle, Long> {

    boolean existsBySourceAndUrl(ImportSource source, String url);

    Page<NewsArticle> findAllByOrderByPublishedAtDesc(Pageable pageable);

    Page<NewsArticle> findBySourceOrderByPublishedAtDesc(ImportSource source, Pageable pageable);
}
