package com.learning.ai.llmragwithspringai;

import com.learning.ai.llmragwithspringai.config.TestcontainersConfiguration;
import org.springframework.boot.SpringApplication;

public class TestLlmRagWithSpringAiApplication {

    public static void main(String[] args) {
        SpringApplication.from(LlmRagWithSpringAiApplication::main)
                .with(TestcontainersConfiguration.class)
                .run(args);
    }
}
