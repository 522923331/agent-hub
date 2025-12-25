package com.agenthub.db.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
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
@Table(
        name = "knowledge_article",
        indexes = {
                @Index(name = "uk_knowledge_article_url", columnList = "url", unique = true)
        }
)
public class KnowledgeArticleEntity {
    @Id
    private UUID id;

    @Column(name = "subscription_id")
    private UUID subscriptionId;

    @Column(name = "source_name", length = 200)
    private String sourceName;

    @Column(nullable = false, length = 2000)
    private String url;

    @Column(length = 600)
    private String title;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "fetched_at")
    private Instant fetchedAt;

    @Column(name = "raw_html", columnDefinition = "TEXT")
    private String rawHtml;

    @Column(name = "extracted_text", columnDefinition = "TEXT")
    private String extractedText;

    @Column(name = "detected_lang", length = 32)
    private String detectedLang;

    @Column(name = "zh_title", length = 600)
    private String zhTitle;

    @Column(name = "zh_content", columnDefinition = "TEXT")
    private String zhContent;

    @Column(columnDefinition = "TEXT")
    private String reflection;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

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


