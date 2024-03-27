package com.learning.ai.llmragwithspringai.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(value = "spring.ai.openai.api-key", havingValue = "demo")
public class ResponseHeadersModification {

    @Bean
    RestClient.Builder restClientBuilder() {
        return RestClient.builder().requestInterceptor((request, body, execution) -> {
            ClientHttpResponse response = execution.execute(request, body);
            return new CustomClientHttpResponse(response);
        });
    }

    private static class CustomClientHttpResponse implements ClientHttpResponse {

        private final ClientHttpResponse originalResponse;
        private final HttpHeaders headers;

        public CustomClientHttpResponse(ClientHttpResponse originalResponse) {
            this.originalResponse = originalResponse;
            MultiValueMap<String, String> modifiedHeaders = new LinkedMultiValueMap<>(originalResponse.getHeaders());
            modifiedHeaders.put(HttpHeaders.CONTENT_TYPE, Collections.singletonList(MediaType.APPLICATION_JSON_VALUE));
            this.headers = new HttpHeaders(modifiedHeaders);
        }

        @Override
        public HttpStatusCode getStatusCode() throws IOException {
            return originalResponse.getStatusCode();
        }

        @Override
        public String getStatusText() throws IOException {
            return originalResponse.getStatusText();
        }

        @Override
        public void close() {}

        @Override
        public InputStream getBody() throws IOException {
            return originalResponse.getBody();
        }

        @Override
        public HttpHeaders getHeaders() {
            return headers;
        }
    }
}
