package rs.serbianelection2026.backend.news.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import rs.serbianelection2026.backend.ingestion.entity.ImportSource;
import rs.serbianelection2026.backend.news.dto.NewsArticleResponse;
import rs.serbianelection2026.backend.news.dto.PageResponse;
import rs.serbianelection2026.backend.news.mapper.NewsArticleMapper;
import rs.serbianelection2026.backend.news.service.NewsService;

@RestController
@RequestMapping("/api/v1/news")
public class NewsController {

    private final NewsService newsService;
    private final NewsArticleMapper newsArticleMapper;

    public NewsController(NewsService newsService, NewsArticleMapper newsArticleMapper) {
        this.newsService = newsService;
        this.newsArticleMapper = newsArticleMapper;
    }

    @GetMapping
    public PageResponse<NewsArticleResponse> getNews(
            @RequestParam(required = false) ImportSource source,
            @PageableDefault(size = 20) Pageable pageable) {
        return PageResponse.of(newsService.getNews(source, pageable).map(newsArticleMapper::toResponse));
    }
}
