package com.learning.ai.config;

import ai.docling.testcontainers.serve.DoclingServeContainer;
import ai.docling.testcontainers.serve.config.DoclingServeContainerConfig;
import java.time.Duration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.postgresql.PostgreSQLContainer;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer("pgvector/pgvector:pg18").withStartupTimeout(Duration.ofMinutes(2));
    }

    @Bean
    DoclingServeContainer doclingServeContainer() {
        return new DoclingServeContainer(DoclingServeContainerConfig.builder()
                .image("ghcr.io/docling-project/docling-serve:v1.9.0")
                .enableUi(true)
                .build());
    }

    @Bean
    DynamicPropertyRegistrar dynamicPropertyRegistrar(DoclingServeContainer doclingServeContainer) {
        // Ensure the container is started and the API URL is available
        return registry -> {
            registry.add("docling.server.url", doclingServeContainer::getApiUrl);
        };
    }
}
