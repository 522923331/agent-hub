package com.agenthub.db.repo;

import com.agenthub.db.entity.KnowledgeArticleEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface KnowledgeArticleRepository extends JpaRepository<KnowledgeArticleEntity, UUID> {
    boolean existsByUrl(String url);

    Optional<KnowledgeArticleEntity> findByUrl(String url);

    Page<KnowledgeArticleEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);
}


