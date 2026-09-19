package com.learning.ai;

import com.learning.ai.config.ContainersConfig;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration(proxyBeanMethods = false)
@ImportTestcontainers(ContainersConfig.class)
class TestLLMRagWithSpringBoot {
    @Bean
    @Primary
    ChatModel mockChatModel() {
        return new ChatModel() {
            @Override
            public ChatResponse chat(ChatRequest request) {
                String msg = request.messages().getLast().toString();
                String json = "{\"name\":\"Unknown\"}";
                if (msg.contains("Rohit") && msg.contains("Sharma")) {
                    json = "{\"name\":\"Rohit Gurunath Sharma\"}";
                }
                return ChatResponse.builder().aiMessage(AiMessage.from(json)).build();
            }
        };
    }

    public static void main(String[] args) {
        SpringApplication.from(LLMRagWithSpringBoot::main)
                .with(TestLLMRagWithSpringBoot.class)
                .run(args);
    }
}
