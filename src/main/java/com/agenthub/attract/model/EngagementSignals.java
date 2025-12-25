package com.agenthub.attract.model;

public record EngagementSignals(
        Integer viewCount,
        Integer commentCount,
        Integer likeCount,
        Integer shareCount,
        Integer hnPoints
) {
    public static EngagementSignals empty() {
        return new EngagementSignals(null, null, null, null, null);
    }
}


