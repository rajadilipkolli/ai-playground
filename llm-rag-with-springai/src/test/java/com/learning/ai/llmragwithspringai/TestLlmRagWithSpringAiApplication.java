package com.learning.ai.llmragwithspringai;

import java.io.IOException;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.ollama.OllamaContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class TestLlmRagWithSpringAiApplication {

	@Bean
	@ServiceConnection
	PostgreSQLContainer<?> pgvectorContainer() {
		return new PostgreSQLContainer<>(DockerImageName.parse("pgvector/pgvector:pg16"));
	}

	@Bean
	OllamaContainer ollama(DynamicPropertyRegistry properties) throws UnsupportedOperationException, IOException, InterruptedException {
		OllamaContainer ollama = new OllamaContainer(DockerImageName.parse("ghcr.io/thomasvitale/ollama-llama2").asCompatibleSubstituteFor("ollama/ollama"));
		properties.add("spring.ai.ollama.base-url", ollama::getEndpoint);
		return ollama;
	}

	public static void main(String[] args) {
		SpringApplication.from(LlmRagWithSpringAiApplication::main).with(TestLlmRagWithSpringAiApplication.class).run(args);
	}

}
