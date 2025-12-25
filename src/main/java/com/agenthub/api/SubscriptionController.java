package com.agenthub.api;

import com.agenthub.db.entity.KnowledgeSubscriptionEntity;
import com.agenthub.db.repo.KnowledgeSubscriptionRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {
    private final KnowledgeSubscriptionRepository repo;

    @GetMapping
    public PageResponse<KnowledgeSubscriptionEntity> list(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "enabled", required = false) Boolean enabled
    ) {
        int safeSize = Math.min(Math.max(size, 1), 200);
        int safePage = Math.max(page, 0);
        Page<KnowledgeSubscriptionEntity> p = repo.search(q, enabled, PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "id")));
        return PageResponse.from(p);
    }

    @PostMapping
    public KnowledgeSubscriptionEntity create(@Valid @RequestBody CreateSubscriptionRequest req) {
        KnowledgeSubscriptionEntity e = new KnowledgeSubscriptionEntity();
        e.setName(req.name());
        e.setZhName(req.zhName());
        e.setCapabilitySummary(req.capabilitySummary());
        e.setSourceType(req.sourceType());
        e.setFeedUrl(req.feedUrl());
        e.setEnabled(req.enabledValue());
        e.setFetchLimit(req.fetchLimitValue());
        e.setTags(req.tags());
        return repo.save(e);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable("id") Long id, @Valid @RequestBody UpdateSubscriptionRequest req) {
        return repo.findById(id)
                .<ResponseEntity<?>>map(e -> {
                    if (req.name() != null) e.setName(req.name());
                    if (req.zhName() != null) e.setZhName(req.zhName());
                    if (req.capabilitySummary() != null) e.setCapabilitySummary(req.capabilitySummary());
                    if (req.sourceType() != null) e.setSourceType(req.sourceType());
                    if (req.feedUrl() != null) e.setFeedUrl(req.feedUrl());
                    if (req.enabled() != null) e.setEnabled(req.enabled());
                    if (req.fetchLimit() != null) e.setFetchLimit(req.fetchLimit());
                    if (req.tags() != null) e.setTags(req.tags());
                    return ResponseEntity.ok(repo.save(e));
                })
                .orElseGet(() -> ResponseEntity.status(404).body(Map.of("error", "not found")));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable("id") Long id) {
        if (!repo.existsById(id)) {
            return ResponseEntity.status(404).body(Map.of("error", "not found"));
        }
        repo.deleteById(id);
        return ResponseEntity.ok(Map.of("deleted", true));
    }

    public record CreateSubscriptionRequest(
            @NotBlank String name,
            @NotBlank String zhName,
            @NotBlank String capabilitySummary,
            @NotBlank String sourceType,
            @NotBlank String feedUrl,
            Boolean enabled,
            @Min(1) @Max(200) Integer fetchLimit,
            String tags
    ) {
        public boolean enabledValue() { return enabled != null ? enabled : true; }
        public int fetchLimitValue() { return fetchLimit == null ? 20 : fetchLimit; }
    }

    public record UpdateSubscriptionRequest(
            String name,
            String zhName,
            String capabilitySummary,
            String sourceType,
            String feedUrl,
            Boolean enabled,
            @Min(1) @Max(200) Integer fetchLimit,
            String tags
    ) {}
}


