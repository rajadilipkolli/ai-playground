package com.example.chatbot.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.VectorStoreChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class ChatConfig {

    @Bean
    ChatClient chatClient(ChatClient.Builder chatClientBuilder, ChatMemory chatMemory, VectorStore vectorStore) {
        return chatClientBuilder
                .clone()
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        VectorStoreChatMemoryAdvisor.builder(vectorStore).build(),
                        new QuestionAnswerAdvisor(vectorStore) // RAG advisor
                        )
                .build();
    }
}
