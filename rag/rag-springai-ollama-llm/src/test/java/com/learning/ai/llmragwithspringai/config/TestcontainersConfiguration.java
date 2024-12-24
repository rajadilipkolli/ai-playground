package com.learning.ai.llmragwithspringai.config;

import java.time.Duration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.grafana.LgtmStackContainer;
import org.testcontainers.ollama.OllamaContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    OllamaContainer ollama() {
        return new OllamaContainer(DockerImageName.parse("ollama/ollama"));
    }

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgreSQLContainer() {
        return new PostgreSQLContainer<>(DockerImageName.parse("pgvector/pgvector:pg17"))
                .withStartupTimeout(Duration.ofMinutes(2));
    }

    @Bean
    @ServiceConnection
    LgtmStackContainer lgtmStackContainer() {
        return new LgtmStackContainer(DockerImageName.parse("grafana/otel-lgtm").withTag("0.8.1"))
                .withStartupTimeout(Duration.ofMinutes(2));
    }
}
