package com.agenthub.knowledge.source;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class KnowledgeSourceRegistry {
    private final Map<String, KnowledgeSource> sourcesByType;

    public KnowledgeSourceRegistry(List<KnowledgeSource> sources) {
        this.sourcesByType = sources.stream()
                .collect(Collectors.toMap(KnowledgeSource::type, Function.identity(), (a, b) -> a));
    }

    public Optional<KnowledgeSource> findByType(String type) {
        if (type == null) return Optional.empty();
        return Optional.ofNullable(sourcesByType.get(type));
    }
}


