package com.learning.ai.config;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import java.net.URI;
import org.springframework.boot.autoconfigure.jdbc.JdbcConnectionDetails;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class LangChainConfig {

    @Bean
    EmbeddingModel embeddingModel() {
        return new AllMiniLmL6V2EmbeddingModel();
    }

    @Bean
    EmbeddingStore<TextSegment> embeddingStore(JdbcConnectionDetails jdbcConnectionDetails) {
        String jdbcUrl = jdbcConnectionDetails.getJdbcUrl();
        URI uri = URI.create(jdbcUrl.substring(5));
        String host = uri.getHost();
        int dbPort = uri.getPort();
        String path = uri.getPath();
        return PgVectorEmbeddingStore.builder()
                .host(host)
                .port(dbPort)
                .database(path.substring(1))
                .user(jdbcConnectionDetails.getUsername())
                .password(jdbcConnectionDetails.getPassword())
                .table("ai_vector_store")
                .dimension(384)
                .build();
    }
}
