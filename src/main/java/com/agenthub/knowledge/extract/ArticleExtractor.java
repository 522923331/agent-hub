package com.agenthub.knowledge.extract;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ArticleExtractor {

    public String extractText(String html, String url) {
        if (html == null || html.isBlank()) return "";
        Document doc = Jsoup.parse(html, url);
        doc.select("script,style,noscript,svg,canvas,form,nav,header,footer,aside").remove();

        List<String> candidates = List.of("article", "main", "div#content", "div.post", "div.article", "div.entry-content");
        Element best = null;
        int bestLen = -1;

        for (String css : candidates) {
            Elements els = doc.select(css);
            for (Element el : els) {
                String text = normalize(el.text());
                int len = text.length();
                if (len > bestLen) {
                    bestLen = len;
                    best = el;
                }
            }
        }

        String text;
        if (best != null && bestLen > 200) {
            text = best.text();
        } else if (doc.body() != null) {
            text = doc.body().text();
        } else {
            text = doc.text();
        }

        return normalize(text);
    }

    private String normalize(String s) {
        if (s == null) return "";
        return s
                .replace("\u00A0", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}


