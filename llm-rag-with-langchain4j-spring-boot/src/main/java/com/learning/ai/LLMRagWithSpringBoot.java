package com.learning.ai;

import com.learning.ai.config.CustomerSupportAgent;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.Map;

@SpringBootApplication
public class LLMRagWithSpringBoot {

    public static void main(String[] args) {
        SpringApplication.run(LLMRagWithSpringBoot.class, args);
    }

    @Bean
    ApplicationRunner interactiveChatRunner(CustomerSupportAgent agent) {
        return args -> {
            var response = agent.chat("what should I know about the transition to consumer direct care network washington?");
            System.out.println(Map.of("response", response));
        };
    }
}
