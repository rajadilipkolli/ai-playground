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

    public static final RedisStackContainer REDIS_CONTAINER = new RedisStackContainer(
                    DockerImageName.parse("redis/redis-stack").withTag("7.4.0-v1"))
            .withReuse(true)
            .withStartupTimeout(Duration.ofMinutes(2));

    static {
        REDIS_CONTAINER.start();
        System.setProperty("spring.ai.chat.memory.redis.host", REDIS_CONTAINER.getHost());
        System.setProperty("spring.ai.chat.memory.redis.port", String.valueOf(REDIS_CONTAINER.getMappedPort(6379)));
        System.setProperty(
                "spring.ai.vectorstore.redis.uri",
                "redis://" + REDIS_CONTAINER.getHost() + ":" + REDIS_CONTAINER.getMappedPort(6379));
        System.setProperty("spring.data.redis.host", REDIS_CONTAINER.getHost());
        System.setProperty("spring.data.redis.port", String.valueOf(REDIS_CONTAINER.getMappedPort(6379)));
        System.setProperty("spring.data.redis.client-type", "jedis");
    }

    @Bean
    @RestartScope
    RedisStackContainer redisStackContainer() {
        return REDIS_CONTAINER;
    }
}
