package com.example.ai.controller;

import com.example.ai.model.request.AIChatRequest;
import com.example.ai.model.response.AIChatResponse;
import com.example.ai.service.ChatService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
public class ChatController {

    private final ChatService chatService;

    ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/chat")
    AIChatResponse chat(@RequestBody AIChatRequest aiChatRequest) {
        return chatService.chat(aiChatRequest.query());
    }

    @PostMapping("/chat-with-prompt")
    AIChatResponse chatWithPrompt(@RequestBody AIChatRequest aiChatRequest) {
        return chatService.chatWithPrompt(aiChatRequest.query());
    }

    @PostMapping("/chat-with-system-prompt")
    AIChatResponse chatWithSystemPrompt(@RequestBody AIChatRequest aiChatRequest) {
        return chatService.chatWithSystemPrompt(aiChatRequest.query());
    }

    @PostMapping("/sentiment/analyze")
    AIChatResponse sentimentAnalyzer(@RequestBody AIChatRequest aiChatRequest) {
        return chatService.analyzeSentiment(aiChatRequest.query());
    }

    //    @PostMapping("/emebedding-client-conversion")
    //    AIChatResponse chatWithEmbeddingClient(@RequestBody AIChatRequest aiChatRequest) {
    //        return chatService.getEmbeddings(aiChatRequest.query());
    //    }
    //
    //    @GetMapping("/output")
    //    public ActorsFilms generate(@RequestParam(value = "actor", defaultValue = "Jr NTR") String actor) {
    //        return chatService.generateAsBean(actor);
    //    }
    //
    //    @PostMapping("/rag")
    //    AIChatResponse chatUsingRag(@RequestBody AIChatRequest aiChatRequest) {
    //        return chatService.ragGenerate(aiChatRequest.query());
    //    }
    //
    //    @PostMapping("/chat/stream")
    //    AIStreamChatResponse streamChat(@RequestBody AIChatRequest aiChatRequest) {
    //        Flux<String> streamChat = chatService.streamChat(aiChatRequest.query());
    //        return new AIStreamChatResponse(streamChat);
    //    }
}
