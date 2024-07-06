package com.learning.ai.llmragwithspringai.controller;

import com.learning.ai.llmragwithspringai.model.request.AIChatRequest;
import com.learning.ai.llmragwithspringai.model.response.AIChatResponse;
import com.learning.ai.llmragwithspringai.service.AIChatService;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
@Validated
class AiController {

    private final AIChatService aiChatService;

    public AiController(AIChatService aiChatService) {
        this.aiChatService = aiChatService;
    }

    @PostMapping("/chat")
    AIChatResponse ragService(@Valid @RequestBody AIChatRequest aiChatRequest) {
        String chatResponse = aiChatService.chat(aiChatRequest.question());
        return new AIChatResponse(chatResponse);
    }
}
