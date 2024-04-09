package com.learning.ai;

import com.learning.ai.config.ContainersConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;

@TestConfiguration(proxyBeanMethods = false)
@ImportTestcontainers(ContainersConfig.class)
class TestLLMRagWithSpringBoot {

    public static void main(String[] args) {
        SpringApplication.from(LLMRagWithSpringBoot::main)
                .with(TestLLMRagWithSpringBoot.class)
                .run(args);
    }
}
