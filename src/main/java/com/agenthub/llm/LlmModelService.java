package com.agenthub.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LlmModelService {
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final LlmProperties props;

    public LlmModelsResult listModels() throws Exception {
        if (props.getBaseUrl() == null || props.getBaseUrl().isBlank()) {
            throw new IllegalStateException("app.llm.base-url is empty");
        }
        if (props.getApiKey() == null || props.getApiKey().isBlank()) {
            throw new IllegalStateException("app.llm.api-key is empty");
        }

        String resp = webClient.get()
                .uri(trimSlash(props.getBaseUrl()) + "/v1/models")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + props.getApiKey())
                .retrieve()
                .bodyToMono(String.class)
                .block();

        if (resp == null || resp.isBlank()) {
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
        return new LlmModelsResult(props.getBaseUrl(), props.getModel(), models);
    }

    private String trimSlash(String s) {
        if (s == null) return "";
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }

    public record LlmModelsResult(String baseUrl, String currentModel, List<LlmModelItem> models) {}

    public record LlmModelItem(String id, String ownedBy, String object) {}
}


