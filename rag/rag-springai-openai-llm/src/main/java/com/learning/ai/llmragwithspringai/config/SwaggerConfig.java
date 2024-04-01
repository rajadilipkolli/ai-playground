package com.learning.ai.llmragwithspringai.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@OpenAPIDefinition(info = @Info(title = "rag-springai-openai-llm", version = "v1.0.0"), servers = @Server(url = "/"))
public class SwaggerConfig {}
