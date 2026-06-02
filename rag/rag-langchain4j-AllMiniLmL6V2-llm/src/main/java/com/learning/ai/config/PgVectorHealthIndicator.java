package com.learning.ai.config;

import dev.langchain4j.model.embedding.EmbeddingModel;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class PgVectorHealthIndicator implements HealthIndicator {

    private final EmbeddingModel embeddingModel;

    public PgVectorHealthIndicator(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    @Override
    public Health health() {
        try {
            // Check embedding model availability
            var embedding = embeddingModel.embed("health-check").content();

            if (embedding == null || embedding.dimension() == 0) {
                return Health.down()
                        .withDetail("error", "Embedding model returned null or empty vector")
                        .build();
            }

            // The PgVectorEmbeddingStore does not expose a ping method directly in LangChain4j.
            // If the application context started and wired properly, and spring-boot-actuator
            // JDBC health indicator is active, that is usually sufficient.
            // This is an additional check for the embedding model component.
            return Health.up()
                    .withDetail("embeddingModel", embeddingModel.getClass().getSimpleName())
                    .withDetail("embeddingDimension", embedding.dimension())
                    .build();

        } catch (Exception e) {
            return Health.down().withException(e).build();
        }
    }
}
