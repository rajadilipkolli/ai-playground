package com.learning.ai.llmragwithspringai.controller;

import com.learning.ai.llmragwithspringai.agent.api.AgentQuery;
import com.learning.ai.llmragwithspringai.agent.api.AgentResult;
import com.learning.ai.llmragwithspringai.agent.api.Orchestrator;
import com.learning.ai.llmragwithspringai.model.request.AgentRunRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agent")
@ConditionalOnProperty(name = "rag.agent.enabled", havingValue = "true")
public class AgentController {

    private final Orchestrator orchestrator;

    public AgentController(Orchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @PostMapping("/run")
    public ResponseEntity<?> runAgent(@RequestBody AgentRunRequest request) {
        if (request == null || request.query() == null || request.query().isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            AgentQuery query = new AgentQuery(request.query(), request.sessionId());
            AgentResult result = orchestrator.run(query);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.internalServerError()
                    .body(java.util.Map.of(
                            "error",
                            "Agent execution failed",
                            "message",
                            e.getMessage() != null ? e.getMessage() : "Unknown error"));
        }
    }
}
