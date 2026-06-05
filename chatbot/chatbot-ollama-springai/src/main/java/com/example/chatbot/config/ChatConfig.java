package com.example.chatbot.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.VectorStoreChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.redis.RedisVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisPooled;

@Configuration(proxyBeanMethods = false)
public class ChatConfig {

    // Needed till https://github.com/spring-projects/spring-ai/issues/6308 is resolved
    @Bean
    RedisVectorStore vectorStore(
            EmbeddingModel embeddingModel,
            JedisConnectionFactory jedisConnectionFactory,
            @Value("${spring.ai.vectorstore.redis.index}") String indexName,
            @Value("${spring.ai.vectorstore.redis.initialize-schema:false}") boolean initializeSchema) {

        JedisPooled jedisPooled = new JedisPooled(
                new HostAndPort(jedisConnectionFactory.getHostName(), jedisConnectionFactory.getPort()),
                DefaultJedisClientConfig.builder()
                        .ssl(jedisConnectionFactory.isUseSsl())
                        .clientName(jedisConnectionFactory.getClientName())
                        .timeoutMillis(jedisConnectionFactory.getTimeout())
                        .password(jedisConnectionFactory.getPassword())
                        .build());

        return RedisVectorStore.builder(jedisPooled, embeddingModel)
                .indexName(indexName)
                .initializeSchema(initializeSchema)
                .metadataFields(RedisVectorStore.MetadataField.text("conversationId"))
                .build();
    }

    @Bean
    ChatClient chatClient(ChatClient.Builder chatClientBuilder, ChatMemory chatMemory, VectorStore vectorStore) {
        return chatClientBuilder
                .clone()
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        VectorStoreChatMemoryAdvisor.builder(vectorStore).build())
                .build();
    }
}
