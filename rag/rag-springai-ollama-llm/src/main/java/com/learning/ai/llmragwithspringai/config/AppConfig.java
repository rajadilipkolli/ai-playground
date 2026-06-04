package com.learning.ai.llmragwithspringai.config;

import com.learning.ai.llmragwithspringai.rag.join.RRFDocumentJoiner;
import com.learning.ai.llmragwithspringai.rag.retrieval.HybridDocumentRetriever;
import com.learning.ai.llmragwithspringai.rag.retrieval.KeywordDocumentRetriever;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.concurrent.Executor;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.json.JsonMapper;

@Configuration(proxyBeanMethods = false)
public class AppConfig {

    @Value("${rag.chunking.size:300}")
    private int chunkSize;

    @Value("${rag.chunking.minSize:100}")
    private int chunkMinSize;

    // Rationale: We use a default chunkSize of 300 to balance context richness and retrieval precision.
    // Nomic-embed-text supports a large context window, but chunks around 300-500 tokens generally yield the best RAG
    // results.
    // Note on Splitter Differences: Spring AI's TokenTextSplitter differs from LangChain4j's recursive splitter.
    // It does not have a direct 'token overlap' parameter; instead, it uses size constraints
    // and attempts to preserve semantic boundaries (like sentences).
    @Bean
    TokenTextSplitter tokenTextSplitter() {
        return TokenTextSplitter.builder()
                .withChunkSize(chunkSize)
                .withMinChunkSizeChars(chunkMinSize)
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = "rag.retrieval.mode", havingValue = "keyword")
    DocumentRetriever keywordDocumentRetriever(
            JdbcTemplate jdbcTemplate,
            @Value("${rag.retrieval.keyword.topK:3}") int topK,
            JsonMapper jsonMapper,
            MeterRegistry meterRegistry) {
        meterRegistry.counter("rag.retrieval.mode.active", "mode", "keyword").increment();
        return new KeywordDocumentRetriever(jdbcTemplate, topK, jsonMapper);
    }

    @Bean
    @ConditionalOnProperty(name = "rag.retrieval.mode", havingValue = "vector")
    DocumentRetriever vectorDocumentRetriever(
            VectorStore vectorStore,
            @Value("${rag.retrieval.topK:3}") int topK,
            @Value("${rag.retrieval.similarityThreshold:0.6}") double similarityThreshold,
            MeterRegistry meterRegistry) {
        meterRegistry.counter("rag.retrieval.mode.active", "mode", "vector").increment();
        return VectorStoreDocumentRetriever.builder()
                .vectorStore(vectorStore)
                .topK(topK)
                .similarityThreshold(similarityThreshold)
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = "rag.retrieval.mode", havingValue = "hybrid", matchIfMissing = true)
    DocumentRetriever hybridDocumentRetriever(
            JdbcTemplate jdbcTemplate,
            @Value("${rag.retrieval.keyword.topK:3}") int keywordTopK,
            JsonMapper jsonMapper,
            VectorStore vectorStore,
            @Value("${rag.retrieval.topK:3}") int vectorTopK,
            @Value("${rag.retrieval.similarityThreshold:0.6}") double similarityThreshold,
            @Value("${rag.retrieval.rrf.k:60}") int rrfK,
            @Value("${rag.retrieval.hybrid.topK:3}") int hybridTopK,
            Executor executor,
            MeterRegistry meterRegistry) {
        meterRegistry.counter("rag.retrieval.mode.active", "mode", "hybrid").increment();

        var vectorRetriever = VectorStoreDocumentRetriever.builder()
                .vectorStore(vectorStore)
                .topK(vectorTopK)
                .similarityThreshold(similarityThreshold)
                .build();

        var keywordRetriever = new KeywordDocumentRetriever(jdbcTemplate, keywordTopK, jsonMapper);
        var joiner = new RRFDocumentJoiner(rrfK, hybridTopK);

        return new HybridDocumentRetriever(vectorRetriever, keywordRetriever, joiner, executor);
    }
}
