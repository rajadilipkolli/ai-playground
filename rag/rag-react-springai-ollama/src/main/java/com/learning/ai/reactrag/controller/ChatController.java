package com.learning.ai.reactrag.controller;

import com.learning.ai.reactrag.model.request.ChatRequest;
import com.learning.ai.reactrag.model.response.ChatResponse;
import com.learning.ai.reactrag.service.AgenticChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
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

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final AgenticChatService chatService;

    public ChatController(AgenticChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    @Operation(
            summary = "Chat with the agent",
            description =
                    "Sends a query to the agentic chat service which may use tools like current time, calculator, or RAG search to answer.")
    public ResponseEntity<?> chat(
            @RequestBody ChatRequest chatRequest,
            @Parameter(description = "Include diagnostics (retrieved documents and tool calls) in the response")
                    @RequestParam(defaultValue = "false", required = false)
                    boolean diagnostics) {

        if (chatRequest == null
                || chatRequest.query() == null
                || chatRequest.query().trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Bad Request", "message", "Query cannot be null or empty", "field", "query"));
        }

        try {
            ChatResponse response = chatService.chat(chatRequest.query(), diagnostics);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("An error occurred during agentic chat", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error",
                            "Internal Server Error",
                            "message",
                            e.getMessage() != null ? e.getMessage() : "Unknown error",
                            "diagnostics",
                            diagnostics));
        }
    }
}
