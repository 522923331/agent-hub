package com.agenthub.api;

import com.agenthub.db.entity.KnowledgeSubscriptionEntity;
import com.agenthub.db.repo.KnowledgeSubscriptionRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {
    private final KnowledgeSubscriptionRepository repo;

    @GetMapping
    public List<KnowledgeSubscriptionEntity> list() {
        return repo.findAll();
    }

    @PostMapping
    public KnowledgeSubscriptionEntity create(@Valid @RequestBody CreateSubscriptionRequest req) {
        KnowledgeSubscriptionEntity e = new KnowledgeSubscriptionEntity();
        e.setId(UUID.randomUUID());
        e.setName(req.name());
        e.setSourceType(req.sourceType());
        e.setFeedUrl(req.feedUrl());
        e.setEnabled(req.enabledValue());
        e.setFetchLimit(req.fetchLimitValue());
        e.setTags(req.tags());
        return repo.save(e);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable("id") UUID id, @Valid @RequestBody UpdateSubscriptionRequest req) {
        return repo.findById(id)
                .<ResponseEntity<?>>map(e -> {
                    if (req.name() != null) e.setName(req.name());
                    if (req.sourceType() != null) e.setSourceType(req.sourceType());
                    if (req.feedUrl() != null) e.setFeedUrl(req.feedUrl());
                    if (req.enabled() != null) e.setEnabled(req.enabled());
                    if (req.fetchLimit() != null) e.setFetchLimit(req.fetchLimit());
                    if (req.tags() != null) e.setTags(req.tags());
                    return ResponseEntity.ok(repo.save(e));
                })
                .orElseGet(() -> ResponseEntity.status(404).body(Map.of("error", "not found")));
    }

    public record CreateSubscriptionRequest(
            @NotBlank String name,
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
            String sourceType,
            String feedUrl,
            Boolean enabled,
            @Min(1) @Max(200) Integer fetchLimit,
            String tags
    ) {}
}


