package com.agenthub.llm;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.llm")
public class LlmProperties {
    private boolean enabled = false;
    private String baseUrl;
    private String apiKey;
    private String model = "gpt-4o-mini";
    private int timeoutSeconds = 90;
}


