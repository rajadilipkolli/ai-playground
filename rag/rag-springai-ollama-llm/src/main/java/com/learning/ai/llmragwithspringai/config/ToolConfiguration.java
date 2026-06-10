package com.learning.ai.llmragwithspringai.config;

import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class ToolConfiguration {
    private static final Logger log = LoggerFactory.getLogger(ToolConfiguration.class);

    @Bean
    ToolCallback currentDateTool() {
        return FunctionToolCallback.builder("currentDateTool", () -> {
                    log.info("fetching from tool in ollama model");
                    return LocalDate.now().toString();
                })
                .description("Get the current date or as of today.")
                .build();
    }
}
