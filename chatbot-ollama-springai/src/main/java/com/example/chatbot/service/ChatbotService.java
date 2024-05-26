package com.example.chatbot.service;

import com.example.chatbot.model.response.AIChatResponse;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.transformer.ChatServiceContext;
import org.springframework.ai.chat.service.ChatService;
import org.springframework.ai.chat.service.ChatServiceResponse;
import org.springframework.stereotype.Service;

@Service
public class ChatbotService {

    private final ChatService chatService;

    public ChatbotService(ChatService chatService) {
        this.chatService = chatService;
    }

    public AIChatResponse chat(String message) {
        Prompt prompt = new Prompt(new UserMessage(message));
        ChatServiceResponse chatServiceResponse = this.chatService.call(new ChatServiceContext(prompt));
        return new AIChatResponse(
                chatServiceResponse.getChatResponse().getResult().getOutput().getContent());
    }
}
