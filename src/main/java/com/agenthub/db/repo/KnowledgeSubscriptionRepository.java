package com.agenthub.db.repo;

import com.agenthub.db.entity.KnowledgeSubscriptionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface KnowledgeSubscriptionRepository extends JpaRepository<KnowledgeSubscriptionEntity, Long> {
    List<KnowledgeSubscriptionEntity> findByEnabledTrueOrderByNameAsc();

    boolean existsByFeedUrlHash(String feedUrlHash);

    @Query("""
            select s from KnowledgeSubscriptionEntity s
            where (:enabled is null or s.enabled = :enabled)
              and (
                :q is null or :q = '' or
                lower(s.name) like lower(concat('%', :q, '%')) or
                lower(s.zhName) like lower(concat('%', :q, '%')) or
                lower(s.capabilitySummary) like lower(concat('%', :q, '%')) or
                lower(s.feedUrl) like lower(concat('%', :q, '%')) or
                lower(coalesce(s.tags, '')) like lower(concat('%', :q, '%'))
              )
            """)
    Page<KnowledgeSubscriptionEntity> search(@Param("q") String q, @Param("enabled") Boolean enabled, Pageable pageable);
}


