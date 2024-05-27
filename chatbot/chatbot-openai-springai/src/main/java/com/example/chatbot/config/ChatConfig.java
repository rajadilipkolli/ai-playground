package com.example.chatbot.config;

import java.util.List;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryChatServiceListener;
import org.springframework.ai.chat.memory.ChatMemoryRetriever;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.memory.LastMaxTokenSizeContentTransformer;
import org.springframework.ai.chat.memory.SystemPromptChatMemoryAugmentor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.service.ChatService;
import org.springframework.ai.chat.service.PromptTransformingChatService;
import org.springframework.ai.tokenizer.JTokkitTokenCountEstimator;
import org.springframework.ai.tokenizer.TokenCountEstimator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class ChatConfig {

    @Bean
    ChatMemory chatHistory() {
        return new InMemoryChatMemory();
    }

    @Bean
    TokenCountEstimator tokenCountEstimator() {
        return new JTokkitTokenCountEstimator();
    }

    @Bean
    ChatService chatService(ChatModel chatModel, ChatMemory chatMemory, TokenCountEstimator tokenCountEstimator) {
        return PromptTransformingChatService.builder(chatModel)
                .withRetrievers(List.of(new ChatMemoryRetriever(chatMemory)))
                .withContentPostProcessors(List.of(new LastMaxTokenSizeContentTransformer(tokenCountEstimator, 1000)))
                .withAugmentors(List.of(new SystemPromptChatMemoryAugmentor()))
                .withChatServiceListeners(List.of(new ChatMemoryChatServiceListener(chatMemory)))
                .build();
    }
}
