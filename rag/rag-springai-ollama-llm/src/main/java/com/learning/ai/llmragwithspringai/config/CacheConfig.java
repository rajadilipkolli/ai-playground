package com.learning.ai.llmragwithspringai.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.concurrent.TimeUnit;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
@ConditionalOnProperty(name = "rag.cache.enabled", havingValue = "true")
public class CacheConfig {

    private final RagCacheProperties ragCacheProperties;

    public CacheConfig(RagCacheProperties ragCacheProperties) {
        this.ragCacheProperties = ragCacheProperties;
    }

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("retrieval-cache");
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(ragCacheProperties.getTtlSeconds(), TimeUnit.SECONDS)
                .maximumSize(ragCacheProperties.getMaxSize())
                .recordStats());
        return cacheManager;
    }
}
