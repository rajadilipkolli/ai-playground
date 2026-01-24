package com.learning.ai;

import com.learning.ai.config.ContainersConfig;
import org.springframework.boot.SpringApplication;

public class TestPgVectorOpenAIEmbeddingStoreExample {

    public static void main(String[] args) {
        SpringApplication.from(PgVectorOpenAIEmbeddingStoreExample::main)
                .with(ContainersConfig.class)
                .run(args);
    }
}
