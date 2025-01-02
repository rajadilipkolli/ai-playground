package com.example.ai.controller;

import com.example.ai.model.request.AIChatRequest;
import com.example.ai.model.response.AIChatResponse;
import com.example.ai.model.response.AIStreamChatResponse;
import com.example.ai.model.response.ActorsFilms;
import com.example.ai.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/ai")
@Validated
public class ChatController {

    private final ChatService chatService;

    ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/chat")
    AIChatResponse chat(@RequestBody @Valid AIChatRequest aiChatRequest) {
        return chatService.chat(aiChatRequest.query());
    }

    @PostMapping("/chat-with-prompt")
    AIChatResponse chatWithPrompt(@RequestBody @Valid AIChatRequest aiChatRequest) {
        return chatService.chatWithPrompt(aiChatRequest.query());
    }

    @PostMapping("/chat-with-system-prompt")
    AIChatResponse chatWithSystemPrompt(@RequestBody @Valid AIChatRequest aiChatRequest) {
        return chatService.chatWithSystemPrompt(aiChatRequest.query());
    }

    @PostMapping("/sentiment/analyze")
    AIChatResponse sentimentAnalyzer(@RequestBody @Valid AIChatRequest aiChatRequest) {
        return chatService.analyzeSentiment(aiChatRequest.query());
    }

    @PostMapping("/embedding-client-conversion")
    AIChatResponse chatWithEmbeddingClient(@RequestBody @Valid AIChatRequest aiChatRequest) {
        return chatService.getEmbeddings(aiChatRequest.query());
    }

    @GetMapping("/output")
    public ActorsFilms generate(@RequestParam(value = "actor", defaultValue = "Jr NTR") String actor) {
        return chatService.generateAsBean(actor);
    }

    @PostMapping("/rag")
    AIChatResponse chatUsingRag(@RequestBody @Valid AIChatRequest aiChatRequest) {
        return chatService.ragGenerate(aiChatRequest.query());
    }

    @PostMapping("/chat/stream")
    AIStreamChatResponse streamChat(@RequestBody @Valid AIChatRequest aiChatRequest) {
        Flux<String> streamChat = chatService.streamChat(aiChatRequest.query());
        return new AIStreamChatResponse(streamChat);
    }
}
