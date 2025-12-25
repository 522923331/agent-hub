package com.agenthub.attract.service;

import com.agenthub.attract.extract.EngagementExtractor;
import com.agenthub.attract.model.EngagementSignals;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EngagementSignalService {
    private final List<EngagementExtractor> extractors;

    public EngagementSignals extract(String url, String html) {
        return extractWithSource(url, html).signals();
    }

    public ExtractResult extractWithSource(String url, String html) {
        if (extractors == null || extractors.isEmpty()) return new ExtractResult("none", EngagementSignals.empty());
        for (EngagementExtractor ex : extractors) {
            EngagementSignals s = ex.extract(url, html);
            if (s != null) return new ExtractResult(ex.getClass().getSimpleName(), s);
        }
        return new ExtractResult("none", EngagementSignals.empty());
    }

    public record ExtractResult(String extractor, EngagementSignals signals) {}
}


