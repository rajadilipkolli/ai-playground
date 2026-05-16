package com.example.chatbot.service;

import com.example.chatbot.model.request.AIChatRequest;
import com.example.chatbot.model.response.AIChatResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Service;

@Service
public class ChatbotService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChatbotService.class);

    private final ChatClient chatClient;

    public ChatbotService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public AIChatResponse chat(String conversationId, AIChatRequest request) {

        String chatResponse = this.chatClient
                .prompt()
                .user(request.query())
                .advisors(a -> {
                    a.param(ChatMemory.CONVERSATION_ID, conversationId);
                })
                .call()
                .content();
        LOGGER.info("Response :{}", chatResponse);
        return new AIChatResponse(chatResponse, conversationId);
    }
}
