package com.example.chatbot.controller;

import com.example.chatbot.model.request.AIChatRequest;
import com.example.chatbot.model.response.AIChatResponse;
import com.example.chatbot.service.ChatbotService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
class ChatbotController {

    private final ChatbotService chatbotService;

    ChatbotController(ChatbotService chatbotService) {
        this.chatbotService = chatbotService;
    }

    @PostMapping("/chat/{conversationId}")
    AIChatResponse chat(@PathVariable String conversationId, @RequestBody AIChatRequest request) {
        return chatbotService.chat(conversationId, request);
    }
}
