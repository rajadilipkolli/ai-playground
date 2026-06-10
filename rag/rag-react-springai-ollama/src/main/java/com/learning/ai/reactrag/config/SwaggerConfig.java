package com.learning.ai.reactrag.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@OpenAPIDefinition(
        info =
                @Info(
                        title = "React RAG Spring AI Ollama API",
                        description = "Agentic Chat Service combining RAG retrieval and multi-tool calling",
                        version = "v1.0.0"),
        servers = @Server(url = "/"))
public class SwaggerConfig {}
