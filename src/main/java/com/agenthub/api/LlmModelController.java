package com.agenthub.api;

import com.agenthub.llm.LlmModelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/llm")
@RequiredArgsConstructor
public class LlmModelController {
    private final LlmModelService llmModelService;

    @GetMapping("/models")
    public ResponseEntity<?> models() {
        try {
            return ResponseEntity.ok(llmModelService.listModels());
        } catch (Exception e) {
            log.warn("List LLM models failed: {}", e.toString());
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}


