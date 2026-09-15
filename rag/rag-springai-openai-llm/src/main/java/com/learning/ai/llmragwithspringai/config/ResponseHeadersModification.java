package com.learning.ai.llmragwithspringai.config;

import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.client.BufferingClientHttpRequestFactory;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(value = "spring.ai.openai.api-key", havingValue = "demo")
class ResponseHeadersModification {

    @Bean
    RestClientCustomizer restClientCustomizer() {
        return restClientBuilder -> restClientBuilder
                .requestFactory(new BufferingClientHttpRequestFactory(
                        ClientHttpRequestFactoryBuilder.detect().build()))
                .requestInterceptor(new ContentTypeOverrideInterceptor())
                .defaultHeaders(httpHeaders -> {
                    httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                    httpHeaders.setAccept(List.of(MediaType.ALL));
                });
    }
}
