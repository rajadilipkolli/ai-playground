package com.learning.ai.llmragwithspringai;

import com.learning.ai.llmragwithspringai.config.TestcontainersConfiguration;
import org.springframework.boot.SpringApplication;

public class TestOllamaRagSpringAiApplication {

    public static void main(String[] args) {
        SpringApplication.from(OllamaRagSpringAiApplication::main)
                .with(TestcontainersConfiguration.class)
                .run(args);
    }
}
