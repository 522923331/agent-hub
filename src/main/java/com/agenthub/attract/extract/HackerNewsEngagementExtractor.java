package com.agenthub.attract.extract;

import com.agenthub.attract.model.EngagementSignals;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

/**
 * 仅支持 HN item 页面：news.ycombinator.com/item?id=xxxx
 * 解析 points 与 comments。
 */
@Component
public class HackerNewsEngagementExtractor implements EngagementExtractor {
    @Override
    public EngagementSignals extract(String url, String html) {
        if (url == null) return null;
        if (!url.contains("news.ycombinator.com/item")) return null;
        if (html == null || html.isBlank()) return EngagementSignals.empty();

        Document doc = Jsoup.parse(html, url);

        Integer points = null;
        Elements score = doc.select("span.score");
        if (!score.isEmpty()) {
            String t = score.first().text(); // "123 points"
            points = parseLeadingInt(t);
        }

        Integer comments = null;
        Elements commentLinks = doc.select("a:matchesOwn((?i)\\bcomment\\b)");
        // 页面里可能有多个，取最后一个更可能是“xxx comments”
        if (!commentLinks.isEmpty()) {
            String t = commentLinks.last().text(); // "45 comments" / "discuss"
            comments = parseLeadingInt(t);
        }

        return new EngagementSignals(null, comments, null, null, points);
    }

    private Integer parseLeadingInt(String s) {
        if (s == null) return null;
        String t = s.trim();
        int i = 0;
        while (i < t.length() && Character.isDigit(t.charAt(i))) i++;
        if (i == 0) return null;
        try { return Integer.parseInt(t.substring(0, i)); } catch (Exception ignore) { return null; }
    }
}


