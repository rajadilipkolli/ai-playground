package com.learning.ai.config;

import java.time.Duration;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer("pgvector/pgvector:pg18")
                .withStartupTimeout(Duration.ofMinutes(2));
    }

    @Bean
    GenericContainer<?> doclingServeContainer() {
        return new GenericContainer<>("ghcr.io/docling-project/docling-serve:1.9.0")
                .withExposedPorts(5001)
                .waitingFor(Wait.forHttp("/ready").forPort(5001).forStatusCode(200))
                .withStartupTimeout(Duration.ofMinutes(2));
    }

    @Bean
    DynamicPropertyRegistrar dynamicPropertyRegistrar(GenericContainer<?> doclingServeContainer) {
        return registry -> {
            registry.add("docling.server.url", () -> 
                "http://" + doclingServeContainer.getHost() + ":" + doclingServeContainer.getMappedPort(5001));
        };
    }
}
