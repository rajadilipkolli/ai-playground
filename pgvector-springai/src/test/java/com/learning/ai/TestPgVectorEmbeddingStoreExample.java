package com.learning.ai;

import com.learning.ai.config.ContainersConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;

@TestConfiguration(proxyBeanMethods = false)
@ImportTestcontainers(ContainersConfig.class)
class TestPgVectorEmbeddingStoreExample {
  
    public static void main(String[] args) {
        SpringApplication.from(PgVectorEmbeddingStoreExample::main)
                .with(TestPgVectorEmbeddingStoreExample.class)
                .run(args);
    }
}