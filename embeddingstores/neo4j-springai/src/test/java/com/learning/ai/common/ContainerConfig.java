package com.learning.ai.common;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class ContainerConfig {

    @Bean
    @ServiceConnection
    Neo4jContainer<?> neo4jContainer() {
        return new Neo4jContainer<>(DockerImageName.parse("neo4j:2026-bullseye")).withPlugins("apoc");
    }
}
