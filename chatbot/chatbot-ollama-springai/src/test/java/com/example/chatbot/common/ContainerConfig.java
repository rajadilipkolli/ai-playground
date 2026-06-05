package com.example.chatbot.common;

import com.redis.testcontainers.RedisStackContainer;
import java.time.Duration;
import org.springframework.boot.devtools.restart.RestartScope;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.BindMode;
import org.testcontainers.grafana.LgtmStackContainer;
import org.testcontainers.ollama.OllamaContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class ContainerConfig {

    @Bean
    @ServiceConnection
    @RestartScope
    OllamaContainer ollama() {
        return new OllamaContainer(DockerImageName.parse("ollama/ollama"))
                .withReuse(true)
                .withFileSystemBind(System.getProperty("user.home") + "/.ollama", "/root/.ollama", BindMode.READ_WRITE);
    }

    @Bean
    @ServiceConnection
    @RestartScope
    LgtmStackContainer lgtmStackContainer() {
        return new LgtmStackContainer(DockerImageName.parse("grafana/otel-lgtm").withTag("0.28.0"))
                .withStartupTimeout(Duration.ofMinutes(2));
    }

    @Bean
    @ServiceConnection(name = "redis")
    @RestartScope
    RedisStackContainer redisStackContainer() {
        return new RedisStackContainer(
                        DockerImageName.parse("redis/redis-stack").withTag("7.4.0-v1"))
                .withReuse(true)
                .withStartupTimeout(Duration.ofMinutes(2));
    }
}
