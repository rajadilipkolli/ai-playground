package com.learning.ai.config;

import dev.langchain4j.model.embedding.EmbeddingModel;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class PgVectorHealthIndicator implements HealthIndicator {

    private final EmbeddingModel embeddingModel;

    private volatile Health cachedHealth;
    private volatile long cachedAt = 0;
    private static final long TTL_MS = 5000;

    public PgVectorHealthIndicator(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    @Override
    public Health health() {
        if (cachedHealth != null && System.currentTimeMillis() - cachedAt < TTL_MS) {
            return cachedHealth;
        }

        try {
            // Check embedding model availability
            var embedding = embeddingModel.embed("health-check").content();

            if (embedding == null || embedding.dimension() == 0) {
                return Health.down()
                        .withDetail("error", "Embedding model returned null or empty vector")
                        .build();
            }

            Health newHealth = Health.up()
                    .withDetail("embeddingModel", embeddingModel.getClass().getSimpleName())
                    .withDetail("embeddingDimension", embedding.dimension())
                    .build();

            cachedHealth = newHealth;
            cachedAt = System.currentTimeMillis();
            return newHealth;

        } catch (Exception e) {
            return Health.down().withException(e).build();
        }
    }
}
