package com.agenthub.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class LlmModelService {
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final LlmProperties props;

    public LlmModelService(@Qualifier("llmWebClient") WebClient webClient, ObjectMapper objectMapper, LlmProperties props) {
        this.webClient = webClient;
        this.objectMapper = objectMapper;
        this.props = props;
    }

    public LlmModelsResult listModels() throws Exception {
        if (props.getBaseUrl() == null || props.getBaseUrl().isBlank()) {
            throw new IllegalStateException("app.llm.base-url is empty");
        }
        if (props.getApiKey() == null || props.getApiKey().isBlank()) {
            throw new IllegalStateException("app.llm.api-key is empty");
        }

        String url = trimSlash(props.getBaseUrl()) + "/v1/models";
        long start = System.currentTimeMillis();
        String resp = webClient.get()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + props.getApiKey())
                .retrieve()
                .bodyToMono(String.class)
                .block();

        if (resp == null || resp.isBlank()) {
            log.warn("LLM 模型列表返回空响应：baseUrl={}, tookMs={}", props.getBaseUrl(), (System.currentTimeMillis() - start));
            return new LlmModelsResult(props.getBaseUrl(), props.getModel(), List.of());
        }

        JsonNode root = objectMapper.readTree(resp);
        JsonNode data = root.path("data");
        List<LlmModelItem> models = new ArrayList<>();
        if (data.isArray()) {
            for (JsonNode m : data) {
                String id = m.path("id").asText("");
                if (id.isBlank()) continue;
                String ownedBy = m.path("owned_by").asText("");
                String object = m.path("object").asText("");
                models.add(new LlmModelItem(id, ownedBy, object));
            }
        }
        log.info("LLM 模型列表查询成功：baseUrl={}, models={}, tookMs={}", props.getBaseUrl(), models.size(), (System.currentTimeMillis() - start));
        return new LlmModelsResult(props.getBaseUrl(), props.getModel(), models);
    }

    private String trimSlash(String s) {
        if (s == null) return "";
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }

    public record LlmModelsResult(String baseUrl, String currentModel, List<LlmModelItem> models) {}

    public record LlmModelItem(String id, String ownedBy, String object) {}
}


