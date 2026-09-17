package rs.serbianelection2026.backend.news.mapper;

import org.springframework.stereotype.Component;
import rs.serbianelection2026.backend.news.dto.NewsArticleResponse;
import rs.serbianelection2026.backend.news.entity.NewsArticle;

@Component
public class NewsArticleMapper {

    public NewsArticleResponse toResponse(NewsArticle article) {
        return new NewsArticleResponse(
                article.getId(),
                article.getSource().name(),
                article.getTitle(),
                article.getDescription(),
                article.getUrl(),
                article.getImageUrl(),
                article.getPublishedAt(),
                article.getFetchedAt());
    }
}
