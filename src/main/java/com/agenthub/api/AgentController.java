package com.agenthub.api;

import com.agenthub.core.agent.AgentRegistry;
import com.agenthub.core.agent.AgentResult;
import com.agenthub.core.agent.AgentRunnerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/agents")
@RequiredArgsConstructor
public class AgentController {
    private final AgentRegistry registry;
    private final AgentRunnerService runner;

    @GetMapping
    public Map<String, Object> list() {
        return Map.of("agents", registry.listAgentNames());
    }

    @PostMapping("/{name}/run")
    public ResponseEntity<?> run(@PathVariable("name") String name,
                                 @RequestParam Map<String, String> params) {
        try {
            Map<String, Object> attrs = new HashMap<>();
            if (params != null) attrs.putAll(params);
            AgentResult r = runner.runByName(name, attrs);
            return ResponseEntity.ok(r);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }
}


