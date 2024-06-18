package com.example.chatbot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.milvus.MilvusContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class TestChatbotOpenaiApplication {

    @Bean
    @ServiceConnection
    MilvusContainer milvusContainer() {
        return new MilvusContainer(DockerImageName.parse("milvusdb/milvus"));
    }

    public static void main(String[] args) {
        SpringApplication.from(ChatbotOpenaiApplication::main)
                .with(TestChatbotOpenaiApplication.class)
                .run(args);
    }
}
