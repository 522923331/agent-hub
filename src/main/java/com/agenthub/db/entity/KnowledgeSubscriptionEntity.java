package com.agenthub.db.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import com.agenthub.util.Sha256;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "knowledge_subscription")
public class KnowledgeSubscriptionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "zh_name", nullable = false, length = 200)
    private String zhName;

    @Column(name = "capability_summary", nullable = false, length = 600)
    private String capabilitySummary;

    @Column(name = "source_type", nullable = false, length = 50)
    private String sourceType;

    @Column(name = "feed_url", nullable = false, length = 1500)
    private String feedUrl;

    @Column(name = "feed_url_hash", nullable = false, columnDefinition = "CHAR(64)")
    private String feedUrlHash;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "fetch_limit", nullable = false)
    private int fetchLimit = 20;

    @Column(length = 500)
    private String tags;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        ensureFeedHash();
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        ensureFeedHash();
        updatedAt = Instant.now();
    }

    private void ensureFeedHash() {
        if (feedUrl != null && !feedUrl.isBlank()) {
            this.feedUrlHash = Sha256.hex(feedUrl.trim());
        }
    }
}


