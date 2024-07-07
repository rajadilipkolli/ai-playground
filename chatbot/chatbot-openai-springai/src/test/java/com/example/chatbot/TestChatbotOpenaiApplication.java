package com.example.chatbot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.qdrant.QdrantContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class TestChatbotOpenaiApplication {

    @Bean
    @ServiceConnection
    QdrantContainer milvusContainer() {
        return new QdrantContainer(DockerImageName.parse("qdrant/qdrant:v1.7.4"));
    }

    public static void main(String[] args) {
        SpringApplication.from(ChatbotOpenaiApplication::main)
                .with(TestChatbotOpenaiApplication.class)
                .run(args);
    }
}
