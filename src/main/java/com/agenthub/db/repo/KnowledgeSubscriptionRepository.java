package com.agenthub.db.repo;

import com.agenthub.db.entity.KnowledgeSubscriptionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface KnowledgeSubscriptionRepository extends JpaRepository<KnowledgeSubscriptionEntity, UUID> {
    List<KnowledgeSubscriptionEntity> findByEnabledTrueOrderByNameAsc();
}


