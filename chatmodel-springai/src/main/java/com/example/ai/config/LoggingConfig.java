package com.example.ai.config;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.client.BufferingClientHttpRequestFactory;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(value = "spring.ai.openai.api-key", havingValue = "demo")
public class LoggingConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingConfig.class);

    @Bean
    RestClientCustomizer restClientCustomizer() {
        return restClientBuilder -> restClientBuilder
                .requestFactory(new BufferingClientHttpRequestFactory(
                        ClientHttpRequestFactoryBuilder.httpComponents().build()))
                .requestInterceptor(new HttpLoggingInterceptor())
                .defaultHeaders(httpHeaders -> {
                    httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                    httpHeaders.setAccept(List.of(MediaType.ALL));
                });
    }
}
