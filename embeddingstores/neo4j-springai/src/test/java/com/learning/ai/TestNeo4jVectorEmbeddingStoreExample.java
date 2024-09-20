package com.learning.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class TestNeo4jVectorEmbeddingStoreExample {

    @Bean
    @ServiceConnection
    Neo4jContainer<?> neo4jContainer() {
        return new Neo4jContainer<>(DockerImageName.parse("neo4j:5")).withPlugins("apoc");
    }

    public static void main(String[] args) {
        SpringApplication.from(Neo4jVectorEmbeddingStoreExample::main)
                .with(TestNeo4jVectorEmbeddingStoreExample.class)
                .run(args);
    }
}
