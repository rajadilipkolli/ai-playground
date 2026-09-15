package com.learning.ai.config;

import ai.docling.testcontainers.serve.DoclingServeContainer;
import ai.docling.testcontainers.serve.config.DoclingServeContainerConfig;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.postgresql.PostgreSQLContainer;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    private static final Logger POSTGRES_LOG = LoggerFactory.getLogger("postgres");

    private static final Logger DOCLING_LOG = LoggerFactory.getLogger("docling");

    /** Creates the PostgreSQL container used by integration tests. */
    @Bean(initMethod = "start", destroyMethod = "stop")
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {

        PostgreSQLContainer container =
                new PostgreSQLContainer("pgvector/pgvector:pg18").withStartupTimeout(Duration.ofMinutes(2));

        container.start();
        container.followOutput(new Slf4jLogConsumer(POSTGRES_LOG));

        return container;
    }

    @Bean
    DoclingServeContainer doclingServeContainer() {
        return new DoclingServeContainer(DoclingServeContainerConfig.builder()
                .image("ghcr.io/docling-project/docling-serve:v1.32.0")
                .enableUi(true)
                .build());
    }

    /** Registers the running Docling container URL with the Spring test context. */
    @Bean
    DynamicPropertyRegistrar dynamicPropertyRegistrar(DoclingServeContainer doclingServeContainer) {
        return registry -> registry.add("docling.server-url", doclingServeContainer::getApiUrl);
    }
}
