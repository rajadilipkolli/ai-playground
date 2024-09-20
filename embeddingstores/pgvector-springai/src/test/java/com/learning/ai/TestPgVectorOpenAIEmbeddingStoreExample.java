package com.learning.ai;

import com.learning.ai.config.ContainersConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;

@TestConfiguration(proxyBeanMethods = false)
@ImportTestcontainers(ContainersConfig.class)
public class TestPgVectorOpenAIEmbeddingStoreExample {

    public static void main(String[] args) {
        SpringApplication.from(PgVectorOpenAIEmbeddingStoreExample::main)
                .with(TestPgVectorOpenAIEmbeddingStoreExample.class)
                .run(args);
    }
}
