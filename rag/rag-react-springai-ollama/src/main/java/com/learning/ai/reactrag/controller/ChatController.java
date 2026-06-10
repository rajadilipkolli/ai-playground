package com.learning.ai.reactrag.controller;

import com.learning.ai.reactrag.model.request.ChatRequest;
import com.learning.ai.reactrag.model.response.ChatResponse;
import com.learning.ai.reactrag.service.AgenticChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
@Tag(name = "Agentic Chat", description = "Agentic Chat endpoints combining RAG and multi-tool execution")
public class ChatController {

    private final AgenticChatService chatService;

    public ChatController(AgenticChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    @Operation(
            summary = "Chat with the agent",
            description =
                    "Sends a query to the agentic chat service which may use tools like current time, calculator, or RAG search to answer.")
    public ResponseEntity<ChatResponse> chat(
            @RequestBody ChatRequest chatRequest,
            @Parameter(description = "Include diagnostics (retrieved documents and tool calls) in the response")
                    @RequestParam(defaultValue = "false", required = false)
                    boolean diagnostics) {

        ChatResponse response = chatService.chat(chatRequest.query(), diagnostics);
        return ResponseEntity.ok(response);
    }
}
