package com.agenthub.db.repo;

import com.agenthub.db.entity.KnowledgeArticleEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface KnowledgeArticleRepository extends JpaRepository<KnowledgeArticleEntity, Long> {
    boolean existsByUrlHash(String urlHash);

    Optional<KnowledgeArticleEntity> findByUrlHash(String urlHash);

    Page<KnowledgeArticleEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("""
            select a from KnowledgeArticleEntity a
            where (:status is null or a.status = :status)
              and (:subscriptionId is null or a.subscriptionId = :subscriptionId)
              and (
                :q is null or :q = '' or
                lower(coalesce(a.title, '')) like lower(concat('%', :q, '%')) or
                lower(coalesce(a.zhTitle, '')) like lower(concat('%', :q, '%')) or
                lower(coalesce(a.sourceName, '')) like lower(concat('%', :q, '%')) or
                lower(a.url) like lower(concat('%', :q, '%'))
              )
            """)
    Page<KnowledgeArticleEntity> search(
            @Param("q") String q,
            @Param("status") String status,
            @Param("subscriptionId") Long subscriptionId,
            Pageable pageable
    );

    @Query("""
            select a from KnowledgeArticleEntity a
            where a.fetchedAt is not null
              and (:since is null or a.fetchedAt >= :since)
              and (a.attractivenessScore is null or a.scoredAt is null or a.scoredAt < :rescoreBefore)
            """)
    Page<KnowledgeArticleEntity> findCandidatesForScoring(
            @Param("since") java.time.Instant since,
            @Param("rescoreBefore") java.time.Instant rescoreBefore,
            Pageable pageable
    );

    @Query("select max(a.fetchedAt) from KnowledgeArticleEntity a where a.subscriptionId = :subscriptionId")
    Instant findLatestFetchedAtBySubscriptionId(@Param("subscriptionId") Long subscriptionId);
}


