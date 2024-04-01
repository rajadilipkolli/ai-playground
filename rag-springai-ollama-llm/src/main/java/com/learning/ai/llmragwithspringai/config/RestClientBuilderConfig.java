package com.learning.ai.llmragwithspringai.config;

import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
public class RestClientBuilderConfig {

    @Bean
    RestClient.Builder restClientBuilder(JdkClientHttpRequestFactory jdkClientHttpRequestFactory) {
        return RestClient.builder().requestFactory(jdkClientHttpRequestFactory);
    }

    @Bean
    JdkClientHttpRequestFactory jdkClientHttpRequestFactory() {
        JdkClientHttpRequestFactory jdkClientHttpRequestFactory = new JdkClientHttpRequestFactory();
        jdkClientHttpRequestFactory.setReadTimeout(Duration.ofMinutes(5));
        return jdkClientHttpRequestFactory;
    }
}
