package com.learning.ai.llmragwithspringai.controller;

import com.learning.ai.llmragwithspringai.service.AIChatService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AIChatService aiChatService;

    public AiController(AIChatService aiChatService) {
        this.aiChatService = aiChatService;
    }

    @GetMapping("/chat")
    Map<String, String> ragService(@RequestParam String message) {
        String chatResponse = aiChatService.chat(message);
        return Map.of("response", chatResponse);
    }
}
