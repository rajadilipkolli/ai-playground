package com.learning.ai.llmragwithspringai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class TestLlmRagWithSpringAiApplication {

	@Bean
	@ServiceConnection
	PostgreSQLContainer<?> pgvectorContainer() {
		return new PostgreSQLContainer<>(DockerImageName.parse("pgvector/pgvector:pg16"));
	}

	public static void main(String[] args) {
		SpringApplication.from(LlmRagWithSpringAiApplication::main).with(TestLlmRagWithSpringAiApplication.class).run(args);
	}

}
