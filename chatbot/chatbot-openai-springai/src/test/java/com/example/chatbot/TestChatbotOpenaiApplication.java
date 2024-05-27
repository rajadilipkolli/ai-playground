package com.example.chatbot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.TestConfiguration;

@TestConfiguration(proxyBeanMethods = false)
public class TestChatbotOpenaiApplication {

    public static void main(String[] args) {
        SpringApplication.from(ChatbotOpenaiApplication::main)
                .with(TestChatbotOpenaiApplication.class)
                .run(args);
    }
}
