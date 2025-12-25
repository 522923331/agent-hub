package com.agenthub.db.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "knowledge_subscription")
public class KnowledgeSubscriptionEntity {
    @Id
    private UUID id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "source_type", nullable = false, length = 50)
    private String sourceType;

    @Column(name = "feed_url", nullable = false, length = 1500)
    private String feedUrl;

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
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }
}


