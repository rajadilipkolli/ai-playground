package com.example.chatbot.service;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;

import com.example.chatbot.model.request.AIChatRequest;
import com.example.chatbot.model.response.AIChatResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;

@Service
public class ChatbotService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChatbotService.class);

    private final ChatClient chatClient;

    public ChatbotService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public AIChatResponse chat(AIChatRequest request) {
        String conversationId = request.conversationId() == null ? "default" : request.conversationId();

        ChatClient.ChatClientRequest.CallResponseSpec call = this.chatClient
                .prompt()
                .user(request.query())
                .advisors(
                        a -> a.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId).param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 100))
                .call();
        ChatResponse chatResponse = call.chatResponse();
        LOGGER.info("Response :{}", chatResponse.getResult());
        return new AIChatResponse(chatResponse.getResult().getOutput().getContent(), null);
    }
}
