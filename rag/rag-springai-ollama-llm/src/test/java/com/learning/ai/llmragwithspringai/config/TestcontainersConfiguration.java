package com.learning.ai.llmragwithspringai.config;

import com.redis.testcontainers.RedisStackContainer;
import java.io.IOException;
import java.time.Duration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.grafana.LgtmStackContainer;
import org.testcontainers.ollama.OllamaContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    OllamaContainer ollama() throws IOException, InterruptedException {
        // The model name to use (e.g., "orca-mini", "mistral", "llama2", "codellama", "phi", or
        // "tinyllama")
        OllamaContainer ollamaContainer = new OllamaContainer(
                DockerImageName.parse("langchain4j/ollama-mistral:latest").asCompatibleSubstituteFor("ollama/ollama"));
        ollamaContainer.start();
        ollamaContainer.execInContainer("ollama", "pull", "nomic-embed-text");
        return ollamaContainer;
    }

    @Bean
    RedisStackContainer redisContainer(DynamicPropertyRegistry properties) {
        RedisStackContainer redis = new RedisStackContainer(
                RedisStackContainer.DEFAULT_IMAGE_NAME.withTag(RedisStackContainer.DEFAULT_TAG));
        properties.add("spring.ai.vectorstore.redis.uri", () -> "redis://%s:%d"
                .formatted(redis.getHost(), redis.getMappedPort(6379)));
        return redis;
    }

    @Bean
    @Scope("singleton")
    @ServiceConnection("otel/opentelemetry-collector-contrib")
    LgtmStackContainer lgtmStackContainer() {
        return new LgtmStackContainer(DockerImageName.parse("grafana/otel-lgtm").withTag("0.7.1"))
                .withStartupTimeout(Duration.ofMinutes(2));
    }
}
