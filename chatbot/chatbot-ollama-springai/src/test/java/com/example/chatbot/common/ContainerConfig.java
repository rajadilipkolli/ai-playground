package com.example.chatbot.common;

import java.time.Duration;
import org.springframework.boot.devtools.restart.RestartScope;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.grafana.LgtmStackContainer;
import org.testcontainers.ollama.OllamaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class ContainerConfig {

    @Bean
    @ServiceConnection
    @RestartScope
    OllamaContainer ollama() {
        return new OllamaContainer("ollama/ollama").withReuse(true);
    }

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgreSQLContainer() {
        return new PostgreSQLContainer(DockerImageName.parse("pgvector/pgvector:pg18"));
    }

    @Bean
    @ServiceConnection
    LgtmStackContainer lgtmStackContainer() {
        return new LgtmStackContainer(DockerImageName.parse("grafana/otel-lgtm").withTag("0.28.0"))
                .withStartupTimeout(Duration.ofMinutes(2));
    }
}
