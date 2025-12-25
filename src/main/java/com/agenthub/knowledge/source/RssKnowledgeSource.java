package com.agenthub.knowledge.source;

import com.agenthub.db.entity.KnowledgeSubscriptionEntity;
import com.agenthub.knowledge.model.DiscoveredArticle;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import javax.xml.XMLConstants;
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
            String body = webClient.get()
                    .uri(feedUrl)
                    .header(HttpHeaders.USER_AGENT, "Mozilla/5.0 (agent-hub/0.1)")
                    .header(HttpHeaders.ACCEPT, "application/rss+xml, application/atom+xml, application/xml, text/xml, */*")
                    .exchangeToMono(resp -> {
                        MediaType ct = resp.headers().contentType().orElse(null);
                        return resp.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .map(b -> {
                                    if (!resp.statusCode().is2xxSuccessful()) {
                                        log.warn("RSS 拉取返回非 2xx：name={}, url={}, status={}, contentType={}",
                                                subscription.getName(), feedUrl, resp.statusCode().value(), ct);
                                    }
                                    return b;
                                });
                    })
                    .onErrorResume(e -> {
                        log.warn("RSS 拉取失败：name={}, url={}, err={}", subscription.getName(), feedUrl, e.toString());
                        return Mono.just("");
                    })
                    .block();
            if (body == null || body.isBlank()) return List.of();

            String xml = sanitizeXml(body);
            if (!looksLikeFeedXml(xml)) {
                String head = xml.substring(0, Math.min(160, xml.length())).replaceAll("\\s+", " ").trim();
                log.warn("RSS 响应不是 XML feed（已跳过）：name={}, url={}, head={}", subscription.getName(), feedUrl, head);
                return List.of();
            }

            DocumentBuilderFactory dbf = secureDbf();
            Document doc = dbf.newDocumentBuilder()
                    .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
            doc.getDocumentElement().normalize();

            List<DiscoveredArticle> out = new ArrayList<>();
            // 订阅的 fetchLimit 代表“本次希望新增的文章数”，但 RSS 里可能有重复/不可达链接，
            // 所以 discovery 阶段多取一些候选，避免实际新增不足。
            int targetNewLimit = Math.max(1, subscription.getFetchLimit());
            int limit = Math.min(Math.max(1, targetNewLimit * 5), 200);
            if (limit != targetNewLimit) {
                log.debug("RSS discover candidate limit boosted: name={}, targetNewLimit={}, candidateLimit={}",
                        subscription.getName(), targetNewLimit, limit);
            }

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
            log.warn("RSS 解析失败：name={}, url={}, err={}", subscription.getName(), feedUrl, e.toString());
            return List.of();
        }
    }

    private String sanitizeXml(String s) {
        if (s == null) return "";
        String t = s;
        // 去除 UTF-8 BOM
        if (!t.isEmpty() && t.charAt(0) == '\uFEFF') t = t.substring(1);
        // 跳过 XML 之前的异常前缀（例如被注入的垃圾字符）
        int idx = t.indexOf('<');
        if (idx > 0) t = t.substring(idx);
        return t.trim();
    }

    private boolean looksLikeFeedXml(String s) {
        if (s == null) return false;
        String t = s.trim();
        if (t.isEmpty()) return false;
        String head = t.substring(0, Math.min(256, t.length())).toLowerCase();
        // 常见 RSS/Atom 头部
        return head.startsWith("<?xml")
                || head.contains("<rss")
                || head.contains("<feed")
                || head.contains("<rdf:rdf");
    }

    private DocumentBuilderFactory secureDbf() throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        // 禁止外部实体/DTD，避免 XXE
        try { dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true); } catch (Exception ignore) {}
        try { dbf.setFeature("http://xml.org/sax/features/external-general-entities", false); } catch (Exception ignore) {}
        try { dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false); } catch (Exception ignore) {}
        try { dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false); } catch (Exception ignore) {}
        dbf.setXIncludeAware(false);
        dbf.setExpandEntityReferences(false);
        return dbf;
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


