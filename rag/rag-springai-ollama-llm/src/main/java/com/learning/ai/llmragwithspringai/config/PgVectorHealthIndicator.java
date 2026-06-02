package com.learning.ai.llmragwithspringai.config;

import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class PgVectorHealthIndicator implements HealthIndicator {

    private final VectorStore vectorStore;

    private volatile Health cachedHealth;
    private volatile long cachedAt = 0;
    private static final long TTL_MS = 5000;

    public PgVectorHealthIndicator(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Override
    public Health health() {
        if (cachedHealth != null && System.currentTimeMillis() - cachedAt < TTL_MS) {
            return cachedHealth;
        }

        try {
            // Check vector store availability
            vectorStore.similaritySearch(org.springframework.ai.vectorstore.SearchRequest.builder()
                    .query("health-check")
                    .topK(1)
                    .build());

            Health newHealth = Health.up()
                    .withDetail("vectorStore", vectorStore.getClass().getSimpleName())
                    .build();

            cachedHealth = newHealth;
            cachedAt = System.currentTimeMillis();
            return newHealth;

        } catch (Exception e) {
            return Health.down().withException(e).build();
        }
    }
}
