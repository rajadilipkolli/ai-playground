package com.learning.ai;

import com.learning.ai.common.ContainerConfig;
import org.springframework.boot.SpringApplication;

public class TestNeo4jVectorEmbeddingStoreExample {

    public static void main(String[] args) {
        SpringApplication.from(Neo4jVectorEmbeddingStoreExample::main)
                .with(ContainerConfig.class)
                .run(args);
    }
}
