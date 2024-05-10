package com.learning.ai.llmragwithspringai.config;

import java.time.LocalDate;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

@Configuration(proxyBeanMethods = false)
public class FunctionConfiguration {

    private static final Logger log = LoggerFactory.getLogger(FunctionConfiguration.class);

    @Bean
    @Description("Get the current date or as of today.")
    Function<String, LocalDate> currentDateFunction() {
        log.info("fetching from function");
        return unused -> LocalDate.now();
    }
}
