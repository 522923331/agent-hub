package com.agenthub.attract.extract;

import com.agenthub.attract.model.EngagementSignals;

/**
 * 从文章页面中抽取可量化的热度信号（浏览/评论/点赞等）。
 * 返回 null 表示该 extractor 不适用。
 */
public interface EngagementExtractor {
    EngagementSignals extract(String url, String html);
}


