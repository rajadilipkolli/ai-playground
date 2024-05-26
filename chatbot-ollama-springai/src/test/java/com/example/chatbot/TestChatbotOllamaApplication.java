package com.example.chatbot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.ollama.OllamaContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class TestChatbotOllamaApplication {

    @Bean
    @ServiceConnection
    OllamaContainer ollama() {
        return new OllamaContainer(
                DockerImageName.parse("langchain4j/ollama-llama3:latest").asCompatibleSubstituteFor("ollama/ollama"));
    }

    public static void main(String[] args) {
        SpringApplication.from(ChatbotOllamaApplication::main)
                .with(TestChatbotOllamaApplication.class)
                .run(args);
    }
}
