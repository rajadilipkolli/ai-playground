package com.learning.ai.agent.controller;

import com.learning.ai.agent.model.request.AgentRequest;
import com.learning.ai.agent.model.response.AgentResponse;
import com.learning.ai.agent.service.AgentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agent")
@Validated
@Tag(name = "Agent", description = "AI Agent API")
public class AgentController {

    private final AgentService agentService;

    public AgentController(AgentService agentService) {
        this.agentService = agentService;
    }

    @PostMapping("/{conversationId}")
    @Operation(
            summary = "Chat with the agent",
            description = "Send a message to the agent and receive a response, maintaining conversation state.")
    public AgentResponse chat(
            @PathVariable("conversationId") String conversationId, @Valid @RequestBody AgentRequest request) {

        String reply = agentService.chat(conversationId, request.message());
        return new AgentResponse(reply);
    }
}
