package com.agenthub.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableConfigurationProperties(LlmProperties.class)
public class LlmConfig {

    @Bean
    @ConditionalOnProperty(prefix = "app.llm", name = "enabled", havingValue = "true")
    public LanguageModelClient openAiCompatibleChatClient(WebClient webClient, ObjectMapper objectMapper, LlmProperties props) {
        return new OpenAiCompatibleChatClient(webClient, objectMapper, props);
    }
}


