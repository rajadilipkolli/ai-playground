package com.learning.ai.llmragwithspringai;

import static com.redis.testcontainers.RedisStackContainer.DEFAULT_IMAGE_NAME;
import static com.redis.testcontainers.RedisStackContainer.DEFAULT_TAG;

import com.redis.testcontainers.RedisStackContainer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.ollama.OllamaContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class TestLlmRagWithSpringAiApplication {

    @Bean
    @ServiceConnection
    OllamaContainer ollama(DynamicPropertyRegistry properties) {
        // The model name to use (e.g., "orca-mini", "mistral", "llama2", "codellama", "phi", or
        // "tinyllama")
        return new OllamaContainer(
                DockerImageName.parse("langchain4j/ollama-llama3:latest").asCompatibleSubstituteFor("ollama/ollama"));
    }

    @Bean
    RedisStackContainer redisContainer(DynamicPropertyRegistry properties) {
        RedisStackContainer redis = new RedisStackContainer(DEFAULT_IMAGE_NAME.withTag(DEFAULT_TAG));
        properties.add("spring.ai.vectorstore.redis.uri", () -> "redis://%s:%d"
                .formatted(redis.getHost(), redis.getMappedPort(6379)));
        return redis;
    }

    public static void main(String[] args) {
        SpringApplication.from(LlmRagWithSpringAiApplication::main)
                .with(TestLlmRagWithSpringAiApplication.class)
                .run(args);
    }
}
