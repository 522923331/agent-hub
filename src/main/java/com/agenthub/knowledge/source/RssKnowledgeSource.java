package com.agenthub.knowledge.source;

import com.agenthub.db.entity.KnowledgeSubscriptionEntity;
import com.agenthub.knowledge.model.DiscoveredArticle;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

@Slf4j
@Component
@RequiredArgsConstructor
public class RssKnowledgeSource implements KnowledgeSource {
    private final WebClient webClient;

    @Override
    public String type() {
        return "RSS";
    }

    @Override
    public List<DiscoveredArticle> discover(KnowledgeSubscriptionEntity subscription) {
        String feedUrl = subscription.getFeedUrl();
        if (feedUrl == null || feedUrl.isBlank()) return List.of();

        try {
            String xml = webClient.get()
                    .uri(feedUrl)
                    .header("User-Agent", "agent-hub/0.1 (+https://local)")
                    .retrieve()
                    .bodyToMono(String.class)
                    .onErrorResume(e -> {
                        log.warn("RSS fetch failed: name={}, url={}, err={}", subscription.getName(), feedUrl, e.toString());
                        return Mono.just("");
                    })
                    .block();
            if (xml == null || xml.isBlank()) return List.of();

            Document doc = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder()
                    .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
            doc.getDocumentElement().normalize();

            List<DiscoveredArticle> out = new ArrayList<>();
            int limit = Math.max(1, subscription.getFetchLimit());

            NodeList items = doc.getElementsByTagName("item");
            for (int i = 0; i < items.getLength() && out.size() < limit; i++) {
                Node node = items.item(i);
                if (!(node instanceof Element)) continue;
                Element item = (Element) node;

                String title = text(item, "title");
                String link = text(item, "link");
                String pubDate = text(item, "pubDate");
                String desc = firstNonBlank(text(item, "description"), textByTagNameNS(item, "*", "encoded"));
                if (link == null || link.isBlank()) continue;

                out.add(new DiscoveredArticle(
                        subscription.getName(),
                        (title == null || title.isBlank()) ? "(no title)" : title,
                        link.trim(),
                        parsePubDate(pubDate),
                        desc
                ));
            }

            // Atom: <entry>
            if (out.isEmpty()) {
                NodeList entries = doc.getElementsByTagName("entry");
                for (int i = 0; i < entries.getLength() && out.size() < limit; i++) {
                    Node node = entries.item(i);
                    if (!(node instanceof Element)) continue;
                    Element entry = (Element) node;
                    String title = text(entry, "title");
                    String link = atomLink(entry);
                    String updated = firstNonBlank(text(entry, "updated"), text(entry, "published"));
                    String summary = firstNonBlank(text(entry, "summary"), text(entry, "content"));
                    if (link == null || link.isBlank()) continue;
                    out.add(new DiscoveredArticle(
                            subscription.getName(),
                            (title == null || title.isBlank()) ? "(no title)" : title,
                            link.trim(),
                            parseIsoInstant(updated),
                            summary
                    ));
                }
            }

            return out;
        } catch (Exception e) {
            log.warn("RSS parse failed: name={}, url={}, err={}", subscription.getName(), feedUrl, e.toString());
            return List.of();
        }
    }

    private String text(Element e, String tag) {
        NodeList list = e.getElementsByTagName(tag);
        if (list == null || list.getLength() == 0) return null;
        Node n = list.item(0);
        return n == null ? null : n.getTextContent();
    }

    private String textByTagNameNS(Element e, String ns, String localName) {
        NodeList list = e.getElementsByTagNameNS(ns, localName);
        if (list == null || list.getLength() == 0) return null;
        Node n = list.item(0);
        return n == null ? null : n.getTextContent();
    }

    private String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) return a;
        if (b != null && !b.isBlank()) return b;
        return null;
    }

    private Instant parsePubDate(String pubDate) {
        if (pubDate == null || pubDate.isBlank()) return null;
        try {
            return ZonedDateTime.parse(pubDate.trim(), DateTimeFormatter.RFC_1123_DATE_TIME).toInstant();
        } catch (Exception ignore) {
            return null;
        }
    }

    private Instant parseIsoInstant(String t) {
        if (t == null || t.isBlank()) return null;
        try {
            return Instant.parse(t.trim());
        } catch (Exception ignore) {
            return null;
        }
    }

    private String atomLink(Element entry) {
        NodeList links = entry.getElementsByTagName("link");
        if (links == null || links.getLength() == 0) return null;
        for (int i = 0; i < links.getLength(); i++) {
            Node n = links.item(i);
            if (!(n instanceof Element)) continue;
            Element el = (Element) n;
            String href = el.getAttribute("href");
            if (href != null && !href.isBlank()) return href;
            String text = el.getTextContent();
            if (text != null && !text.isBlank()) return text;
        }
        return null;
    }
}


