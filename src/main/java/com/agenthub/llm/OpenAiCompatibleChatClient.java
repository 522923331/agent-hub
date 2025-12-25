package com.agenthub.llm;

import com.agenthub.knowledge.model.EnrichedArticle;
import com.agenthub.knowledge.model.ExtractedArticle;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Slf4j
public class OpenAiCompatibleChatClient implements LanguageModelClient {
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final LlmProperties props;

    public OpenAiCompatibleChatClient(WebClient webClient, ObjectMapper objectMapper, LlmProperties props) {
        this.webClient = webClient;
        this.objectMapper = objectMapper;
        this.props = props;
    }

    @Override
    public EnrichedArticle enrich(ExtractedArticle article) {
        try {
            String system = systemPrompt();
            String user = userPrompt(article);

            Map<String, Object> body = Map.of(
                    "model", props.getModel(),
                    "messages", List.of(
                            Map.of("role", "system", "content", system),
                            Map.of("role", "user", "content", user)
                    ),
                    "temperature", 0.7,
                    "response_format", Map.of("type", "json_object")
            );

            String url = normalizeBaseUrl(props.getBaseUrl()) + "/v1/chat/completions";

            String resp = webClient.post()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, bearer(props.getApiKey()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (resp == null || resp.isBlank()) {
                return fallback(article, "LLM 返回空响应");
            }

            JsonNode root = objectMapper.readTree(resp);
            String content = root.path("choices").path(0).path("message").path("content").asText(null);
            if (content == null || content.isBlank()) {
                return fallback(article, "LLM content 为空");
            }

            JsonNode j = objectMapper.readTree(content);
            String zhTitle = j.path("zhTitle").asText("");
            String zhContent = j.path("zhContent").asText("");
            String reflection = j.path("reflection").asText("");
            if (zhTitle.isBlank()) zhTitle = safeTitle(article);
            if (zhContent.isBlank()) zhContent = safeText(article);
            return new EnrichedArticle(zhTitle, zhContent, reflection);
        } catch (Exception e) {
            log.warn("LLM enrich failed: err={}", e.toString());
            return fallback(article, e.getMessage());
        }
    }

    private EnrichedArticle fallback(ExtractedArticle article, String reason) {
        return new EnrichedArticle(
                safeTitle(article),
                safeText(article),
                "（读后感生成失败：" + (reason == null ? "unknown" : reason) + "）"
        );
    }

    private String safeTitle(ExtractedArticle article) {
        if (article == null || article.discovered() == null) return "";
        return article.discovered().title() == null ? "" : article.discovered().title();
    }

    private String safeText(ExtractedArticle article) {
        if (article == null) return "";
        return article.extractedText() == null ? "" : article.extractedText();
    }

    private String bearer(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) return "";
        return "Bearer " + apiKey;
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null) return "";
        if (baseUrl.endsWith("/")) return baseUrl.substring(0, baseUrl.length() - 1);
        return baseUrl;
    }

    private String systemPrompt() {
        return """
你是一名真实的中文读者与作者。你会阅读一篇外文文章，然后用自然的中文把文章讲清楚，并写一段读后感。

硬性要求：
1) 禁止自称“作为AI/模型/助手”
2) 读后感要像真人：有一点情绪、有具体细节、有自己的取舍与质疑，不要机械总结
3) 不要堆砌套话（例如“总之/综上所述/值得注意的是”过多）
4) 输出必须是严格 JSON（不要额外文字）

输出 JSON 结构：
{
  "zhTitle": "中文标题（自然，不直译，不超过30字）",
  "zhContent": "中文正文（意译为主，可重组结构，保留关键信息与事实）",
  "reflection": "读后感（200~500字，像读完后写给朋友的感想）"
}
""";
    }

    private String userPrompt(ExtractedArticle article) {
        String title = safeTitle(article);
        String url = article == null || article.discovered() == null ? "" : article.discovered().url();
        String summary = article == null || article.discovered() == null ? "" : (article.discovered().summary() == null ? "" : article.discovered().summary());
        String text = safeText(article);

        return """
请阅读下面的文章信息并生成结果：

URL: %s
原标题: %s
摘要/简介(可能含HTML): %s

正文纯文本（可能不完整，但尽量据此理解）:
%s
""".formatted(url, title, summary, text);
    }
}


