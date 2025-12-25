package com.agenthub.knowledge.source;

import com.agenthub.db.entity.KnowledgeSubscriptionEntity;
import com.agenthub.knowledge.model.DiscoveredArticle;

import java.util.List;

public interface KnowledgeSource {
    String type();

    List<DiscoveredArticle> discover(KnowledgeSubscriptionEntity subscription);
}


