package com.learning.ai.config;

import java.io.IOException;
import java.io.InputStream;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(value = "spring.ai.openai.api-key", havingValue = "demo")
class ResponseHeadersModification {

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
            HttpHeaders modifiedHeaders = new HttpHeaders(originalResponse.getHeaders());
            modifiedHeaders.setContentType(MediaType.APPLICATION_JSON);
            this.headers = modifiedHeaders;
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
