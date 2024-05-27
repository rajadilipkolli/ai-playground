package com.example.chatbot.service;

import com.example.chatbot.model.request.AIChatRequest;
import com.example.chatbot.model.response.AIChatResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.transformer.ChatServiceContext;
import org.springframework.ai.chat.service.ChatService;
import org.springframework.ai.chat.service.ChatServiceResponse;
import org.springframework.stereotype.Service;

@Service
public class ChatbotService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChatbotService.class);

    private final ChatService chatService;

    public ChatbotService(ChatService chatService) {
        this.chatService = chatService;
    }

    public AIChatResponse chat(AIChatRequest request) {
        Prompt prompt = new Prompt(new UserMessage(request.query()));
        String conversationId = request.conversationId() == null ? "default" : request.conversationId();
        ChatServiceResponse chatServiceResponse = this.chatService.call(new ChatServiceContext(prompt, conversationId));
        LOGGER.info("Response :{}", chatServiceResponse.getChatResponse());
        return new AIChatResponse(
                chatServiceResponse.getChatResponse().getResult().getOutput().getContent(),
                chatServiceResponse.getPromptContext().getConversationId());
    }
}
