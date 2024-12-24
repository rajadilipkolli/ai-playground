package com.example.chatbot;

import com.example.chatbot.common.ContainerConfig;
import org.springframework.boot.SpringApplication;

public class TestChatbotOllamaApplication {

    public static void main(String[] args) {
        SpringApplication.from(ChatbotOllamaApplication::main)
                .with(ContainerConfig.class)
                .run(args);
    }
}
