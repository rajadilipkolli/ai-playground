package com.example.chatbot.config;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpRequest;
import org.springframework.http.MediaType;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.util.StreamUtils;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(value = "spring.ai.openai.api-key", havingValue = "demo")
public class LoggingConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingConfig.class);

    @Bean
    RestClientCustomizer restClientCustomizer() {
        return restClientBuilder -> restClientBuilder
                .requestFactory(new BufferingClientHttpRequestFactory(new HttpComponentsClientHttpRequestFactory()))
                .requestInterceptor((request, body, execution) -> {
                    logRequest(request, body);
                    ClientHttpResponse response = execution.execute(request, body);
                    logResponse(response);
                    return new CustomClientHttpResponse(response);
                })
                .defaultHeaders(httpHeaders -> {
                    httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                    httpHeaders.setAccept(List.of(MediaType.ALL));
                });
    }

    private void logResponse(ClientHttpResponse response) throws IOException {
        LOGGER.info("============================response begin==========================================");
        LOGGER.info("Status code  : {}", response.getStatusCode());
        LOGGER.info("Status text  : {}", response.getStatusText());
        LOGGER.info("Headers      : {}", response.getHeaders());
        LOGGER.info("Response body: {}", StreamUtils.copyToString(response.getBody(), Charset.defaultCharset()));
        LOGGER.info("=======================response end=================================================");
    }

    private void logRequest(HttpRequest request, byte[] body) {

        LOGGER.info("===========================request begin================================================");
        LOGGER.info("URI         : {}", request.getURI());
        LOGGER.info("Method      : {}", request.getMethod());
        LOGGER.info("Headers     : {}", request.getHeaders());
        LOGGER.info("Request body: {}", new String(body, StandardCharsets.UTF_8));
        LOGGER.info("==========================request end================================================");
    }
}
