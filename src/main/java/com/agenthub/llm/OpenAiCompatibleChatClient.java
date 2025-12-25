package com.agenthub.llm;

import com.agenthub.knowledge.model.EnrichedArticle;
import com.agenthub.knowledge.model.ExtractedArticle;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
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
            return doEnrich(article, false);
        } catch (Exception e) {
            // 针对 ReadTimeout 做一次轻量重试（不阻塞太久）
            if (isTimeout(e)) {
                log.warn("LLM 解读调用超时，重试一次");
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
                try {
                    return doEnrich(article, true);
                } catch (Exception e2) {
                    log.warn("LLM 解读重试失败：err={}", e2.toString());
                    return fallback(article, e2.getMessage());
                }
            }
            log.warn("LLM 解读调用失败：err={}", e.toString());
            return fallback(article, e.getMessage());
        }
    }

    private EnrichedArticle doEnrich(ExtractedArticle article, boolean isRetry) throws Exception {
        String system = systemPrompt();
        String user = userPrompt(article);

        Map<String, Object> body = Map.of(
                "model", props.getModel(),
                "messages", List.of(
                        Map.of("role", "system", "content", system),
                        Map.of("role", "user", "content", user)
                ),
                "temperature", isRetry ? 0.6 : 0.7,
                "response_format", Map.of("type", "json_object")
        );

        String url = normalizeBaseUrl(props.getBaseUrl()) + "/v1/chat/completions";
        int t = Math.max(10, props.getTimeoutSeconds());
        long start = System.currentTimeMillis();

        String resp = webClient.post()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, bearer(props.getApiKey()))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(t))
                .block();

        log.info("LLM 解读调用成功：tookMs={}, retry={}", (System.currentTimeMillis() - start), isRetry);

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
    }

    private boolean isTimeout(Throwable e) {
        if (e == null) return false;
        String n = e.getClass().getName();
        if (n.contains("ReadTimeoutException")) return true;
        String msg = e.getMessage();
        if (msg != null && msg.toLowerCase().contains("timeout")) return true;
        return isTimeout(e.getCause());
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
你是一名真实的中文读者与“解读作者”。你会阅读一篇外文文章，然后写出一篇“中文解读稿”：让不了解背景的读者也能身临其境地理解作者在说什么、为什么这么说、关键逻辑链是什么。

硬性要求：
1) 禁止自称“作为AI/模型/助手”
2) 中文解读稿不是“摘要/总结”：不要只用几段概括，而要把文章内容讲透（可故事化/场景化/类比化/分步骤讲解）
3) 但也不要编造原文不存在的事实、数据、引语；遇到原文没说清的地方，请明确写“原文未说明/无法确定”
4) 语言要像真人写给朋友：有节奏、有画面感、有解释，有必要时给一个生活化例子帮助理解
5) 不要堆砌套话（例如“总之/综上所述/值得注意的是”过多）
4) 输出必须是严格 JSON（不要额外文字）

输出 JSON 结构：
{
  "zhTitle": "中文标题（自然，不直译，不超过30字）",
  "zhContent": "中文解读稿（建议 1200~2500 字；以解释/讲解为主，必要时可重组结构；保留关键事实与逻辑链，并用类比/例子让读者理解）",
  "reflection": "读后感（200~500字，像读完后写给朋友的感想：你认可什么、质疑什么、联想到什么）"
}
""";
    }

    private String userPrompt(ExtractedArticle article) {
        String title = safeTitle(article);
        String url = article == null || article.discovered() == null ? "" : article.discovered().url();
        String summary = article == null || article.discovered() == null ? "" : (article.discovered().summary() == null ? "" : article.discovered().summary());
        String text = safeText(article);

        return """
请基于下面的文章信息，写出“中文解读稿”（不是摘要）。要求：
- 先用 1~2 段把读者带入：这篇文章在讨论什么问题/为什么重要（不要标题党）
- 正文按逻辑讲清楚：概念→论据→推理→结论（必要时做类比/举例）
- 尽量贴近原文信息，不要捏造
- 最后再写读后感

URL: %s
原标题: %s
摘要/简介(可能含HTML): %s

正文纯文本（可能不完整，但尽量据此理解）:
%s
""".formatted(url, title, summary, text);
    }
}


