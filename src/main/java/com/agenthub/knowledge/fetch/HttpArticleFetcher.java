package com.agenthub.knowledge.fetch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class HttpArticleFetcher {
    private final WebClient webClient;

    public String fetchHtml(String url) {
        try {
            return webClient.get()
                    .uri(url)
                    .header(HttpHeaders.USER_AGENT, "Mozilla/5.0 (agent-hub/0.1)")
                    .header(HttpHeaders.ACCEPT, MediaType.TEXT_HTML_VALUE + ",application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (Exception e) {
            log.warn("拉取 HTML 失败：url={}, err={}", url, e.toString());
            throw e;
        }
    }
}


