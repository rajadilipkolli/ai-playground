package com.learning.ai;

import com.learning.ai.config.DoclingProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(DoclingProperties.class)
public class RagLangchain4jDoclingApplication {

    /** Starts the Spring Boot application. */
    public static void main(String[] args) {
        SpringApplication.run(RagLangchain4jDoclingApplication.class, args);
    }
}
