package com.agenthub.attract.extract;

import com.agenthub.attract.model.EngagementSignals;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

/**
 * 尝试从 JSON-LD（application/ld+json）中抽取 commentCount / interactionCount 等字段。
 * 这是相对通用的一种方式（不少媒体站点会埋该字段）。
 */
@Component
public class JsonLdEngagementExtractor implements EngagementExtractor {
    private final ObjectMapper om = new ObjectMapper();

    @Override
    public EngagementSignals extract(String url, String html) {
        if (html == null || html.isBlank()) return null;
        try {
            Document doc = Jsoup.parse(html, url);
            for (Element el : doc.select("script[type=application/ld+json]")) {
                String json = el.data();
                if (json == null || json.isBlank()) json = el.html();
                if (json == null || json.isBlank()) continue;
                JsonNode root = om.readTree(json);
                EngagementSignals sig = scan(root);
                if (sig != null) return sig;
            }
        } catch (Exception ignore) {
            return null;
        }
        return null;
    }

    private EngagementSignals scan(JsonNode n) {
        if (n == null || n.isNull()) return null;
        if (n.isArray()) {
            for (JsonNode x : n) {
                EngagementSignals s = scan(x);
                if (s != null) return s;
            }
            return null;
        }

        Integer commentCount = intOrNull(n, "commentCount");
        Integer viewCount = intOrNull(n, "viewCount");

        // interactionStatistic 里可能有 userInteractionCount
        JsonNode interaction = n.get("interactionStatistic");
        Integer likeCount = null;
        Integer shareCount = null;
        if (interaction != null) {
            if (interaction.isArray()) {
                for (JsonNode it : interaction) {
                    String type = text(it, "@type");
                    Integer c = intOrNull(it, "userInteractionCount");
                    if (type != null && c != null) {
                        String tl = type.toLowerCase();
                        if (tl.contains("like")) likeCount = c;
                        if (tl.contains("share")) shareCount = c;
                        if (tl.contains("comment")) commentCount = commentCount != null ? commentCount : c;
                    }
                }
            } else if (interaction.isObject()) {
                Integer c = intOrNull(interaction, "userInteractionCount");
                String type = text(interaction, "@type");
                if (type != null && c != null) {
                    String tl = type.toLowerCase();
                    if (tl.contains("like")) likeCount = c;
                    if (tl.contains("share")) shareCount = c;
                    if (tl.contains("comment")) commentCount = commentCount != null ? commentCount : c;
                }
            }
        }

        boolean any = commentCount != null || viewCount != null || likeCount != null || shareCount != null;
        if (any) return new EngagementSignals(viewCount, commentCount, likeCount, shareCount, null);

        // 递归扫描子节点
        for (var it = n.fields(); it.hasNext(); ) {
            var e = it.next();
            EngagementSignals s = scan(e.getValue());
            if (s != null) return s;
        }
        return null;
    }

    private Integer intOrNull(JsonNode n, String field) {
        if (n == null) return null;
        JsonNode v = n.get(field);
        if (v == null || v.isNull()) return null;
        if (v.isInt() || v.isLong()) return v.asInt();
        if (v.isTextual()) {
            try { return Integer.parseInt(v.asText().trim()); } catch (Exception ignore) { return null; }
        }
        return null;
    }

    private String text(JsonNode n, String field) {
        if (n == null) return null;
        JsonNode v = n.get(field);
        if (v == null || v.isNull()) return null;
        return v.asText(null);
    }
}


