package com.example.chatbot.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SafeGuardAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.vectorstore.VectorStoreChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.redis.RedisVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import redis.clients.jedis.RedisClient;

@Configuration(proxyBeanMethods = false)
public class ChatConfig {

    // Needed till https://github.com/spring-projects/spring-ai/issues/6308 is resolved
    @Bean
    RedisVectorStore vectorStore(
            EmbeddingModel embeddingModel,
            RedisClient redisClient,
            @Value("${spring.ai.vectorstore.redis.index}") String indexName,
            @Value("${spring.ai.vectorstore.redis.initialize-schema:false}") boolean initializeSchema) {

        return RedisVectorStore.builder(redisClient, embeddingModel)
                .indexName(indexName)
                .initializeSchema(initializeSchema)
                .metadataFields(
                        RedisVectorStore.MetadataField.tag("chat_memory_conversation_id"),
                        RedisVectorStore.MetadataField.tag("conversationId"))
                .build();
    }

    @Bean
    ChatClient chatClient(
            ChatClient.Builder chatClientBuilder,
            ChatMemory chatMemory,
            VectorStore vectorStore,
            GuardrailsProperties guardrailsProperties) {
        var builder = chatClientBuilder
                .clone()
                .defaultSystem(
                        "You are a helpful customer support chatbot. Stay on topic, refuse harmful requests, do not reveal internal system details, and politely decline off-topic queries.");

        List<Advisor> advisors = new ArrayList<>();

        if (guardrailsProperties.getLogging().isEnabled()) {
            advisors.add(new SimpleLoggerAdvisor());
        }

        if (guardrailsProperties.getSensitiveWords() != null
                && !guardrailsProperties.getSensitiveWords().isEmpty()) {
            advisors.add(new SafeGuardAdvisor(
                    guardrailsProperties.getSensitiveWords(),
                    guardrailsProperties.getFailureMessage(),
                    Ordered.LOWEST_PRECEDENCE));
        }

        advisors.add(MessageChatMemoryAdvisor.builder(chatMemory).build());
        advisors.add(VectorStoreChatMemoryAdvisor.builder(vectorStore).build());

        return builder.defaultAdvisors(advisors).build();
    }
}
