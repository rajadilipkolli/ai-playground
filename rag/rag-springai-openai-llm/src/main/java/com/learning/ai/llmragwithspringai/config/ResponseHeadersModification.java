package com.learning.ai.llmragwithspringai.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StreamUtils;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(value = "spring.ai.openai.api-key", havingValue = "demo")
public class ResponseHeadersModification {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResponseHeadersModification.class);

    @Bean
    RestClientCustomizer restClientCustomizer() {
        return restClientBuilder -> restClientBuilder
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
