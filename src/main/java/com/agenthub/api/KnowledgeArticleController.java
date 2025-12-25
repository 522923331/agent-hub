package com.agenthub.api;

import com.agenthub.db.entity.KnowledgeArticleEntity;
import com.agenthub.db.repo.KnowledgeArticleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
public class KnowledgeArticleController {
    private final KnowledgeArticleRepository repo;

    @GetMapping
    public PageResponse<ArticleListItem> list(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "subscriptionId", required = false) Long subscriptionId
    ) {
        int safeSize = Math.min(Math.max(size, 1), 200);
        int safePage = Math.max(page, 0);
        log.info("查询文章列表：page={}, size={}, q={}, status={}, subscriptionId={}", safePage, safeSize, q, status, subscriptionId);
        Page<KnowledgeArticleEntity> p = repo.search(q, status, subscriptionId, PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "id")));
        Page<ArticleListItem> mapped = p.map(ArticleListItem::from);
        return PageResponse.from(mapped);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable("id") Long id) {
        return repo.findById(id)
                .<ResponseEntity<?>>map(a -> ResponseEntity.ok(ArticleDetail.from(a)))
                .orElseGet(() -> ResponseEntity.status(404).body(Map.of("error", "not found")));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable("id") Long id, @Valid @RequestBody UpdateArticleRequest req) {
        log.info("修改文章：id={}, fields=[zhTitle={}, zhContent={}, reflection={}, status={}]",
                id, req.zhTitle() != null, req.zhContent() != null, req.reflection() != null, req.status() != null);
        return repo.findById(id)
                .<ResponseEntity<?>>map(a -> {
                    if (req.zhTitle() != null) a.setZhTitle(req.zhTitle());
                    if (req.zhContent() != null) a.setZhContent(req.zhContent());
                    if (req.reflection() != null) a.setReflection(req.reflection());
                    if (req.status() != null) a.setStatus(req.status());
                    KnowledgeArticleEntity saved = repo.save(a);
                    log.info("修改文章成功：id={}, status={}", saved.getId(), saved.getStatus());
                    return ResponseEntity.ok(ArticleDetail.from(saved));
                })
                .orElseGet(() -> ResponseEntity.status(404).body(Map.of("error", "not found")));
    }

    public record ArticleListItem(
            Long id,
            Long subscriptionId,
            String sourceName,
            String url,
            String title,
            String zhTitle,
            String status,
            java.math.BigDecimal attractivenessScore,
            java.time.Instant publishedAt,
            java.time.Instant fetchedAt,
            java.time.Instant createdAt
    ) {
        public static ArticleListItem from(KnowledgeArticleEntity a) {
            return new ArticleListItem(
                    a.getId(),
                    a.getSubscriptionId(),
                    a.getSourceName(),
                    a.getUrl(),
                    a.getTitle(),
                    a.getZhTitle(),
                    a.getStatus(),
                    a.getAttractivenessScore(),
                    a.getPublishedAt(),
                    a.getFetchedAt(),
                    a.getCreatedAt()
            );
        }
    }

    public record ArticleDetail(
            Long id,
            Long subscriptionId,
            String sourceName,
            String url,
            String title,
            String zhTitle,
            String zhContent,
            String reflection,
            String status,
            java.time.Instant publishedAt,
            java.time.Instant fetchedAt,
            java.time.Instant createdAt,
            java.time.Instant updatedAt,
            String errorMessage
    ) {
        public static ArticleDetail from(KnowledgeArticleEntity a) {
            return new ArticleDetail(
                    a.getId(),
                    a.getSubscriptionId(),
                    a.getSourceName(),
                    a.getUrl(),
                    a.getTitle(),
                    a.getZhTitle(),
                    a.getZhContent(),
                    a.getReflection(),
                    a.getStatus(),
                    a.getPublishedAt(),
                    a.getFetchedAt(),
                    a.getCreatedAt(),
                    a.getUpdatedAt(),
                    a.getErrorMessage()
            );
        }
    }

    public record UpdateArticleRequest(
            String zhTitle,
            String zhContent,
            String reflection,
            String status
    ) {}
}


