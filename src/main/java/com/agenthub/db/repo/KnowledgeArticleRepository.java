package com.agenthub.db.repo;

import com.agenthub.db.entity.KnowledgeArticleEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface KnowledgeArticleRepository extends JpaRepository<KnowledgeArticleEntity, Long> {
    boolean existsByUrlHash(String urlHash);

    Optional<KnowledgeArticleEntity> findByUrlHash(String urlHash);

    Page<KnowledgeArticleEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);
}


