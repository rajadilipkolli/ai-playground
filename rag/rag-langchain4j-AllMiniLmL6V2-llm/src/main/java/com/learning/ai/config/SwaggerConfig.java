package com.learning.ai.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@OpenAPIDefinition(
        info = @Info(title = "rag-langchain4j-AllMiniLmL6V2-llm", version = "v1.0.0"),
        servers = @Server(url = "/"))
class SwaggerConfig {}
