package com.learning.ai;

import com.learning.ai.config.AICustomerSupportAgent;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class LLMRagWithSpringBoot {

    public static void main(String[] args) {
        SpringApplication.run(LLMRagWithSpringBoot.class, args);
    }

    @Bean
    ApplicationRunner interactiveChatRunner(AICustomerSupportAgent agent) {
        return args -> {
            var response =
                    agent.chat("what should I know about the transition to consumer direct care network washington?");
            System.out.println(response);
        };
    }
}
