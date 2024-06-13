package com.example.chatbot.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

public class CustomClientHttpResponse implements ClientHttpResponse {

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
