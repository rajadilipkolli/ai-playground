package com.learning.ai.llmragwithspringai.controller;

import com.learning.ai.llmragwithspringai.model.request.AIChatRequest;
import com.learning.ai.llmragwithspringai.model.response.AIChatResponse;
import com.learning.ai.llmragwithspringai.service.AIChatService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
@Validated
class AiController {

    private final AIChatService aiChatService;

    public AiController(AIChatService aiChatService) {
        this.aiChatService = aiChatService;
    }

    @Operation(
            summary = "Chat with AI",
            description =
                    "Ask a question and optionally filter the context by category, documentType, owner, or other custom filters mapping.")
    @PostMapping("/chat")
    AIChatResponse chat(
            @RequestBody @Valid AIChatRequest aiChatRequest,
            @RequestParam(defaultValue = "false") boolean includeDiagnostics) {
        return aiChatService.chat(aiChatRequest, includeDiagnostics);
    }
}
