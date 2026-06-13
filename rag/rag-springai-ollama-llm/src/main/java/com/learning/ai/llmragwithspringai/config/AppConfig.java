package com.learning.ai.llmragwithspringai.config;

import com.learning.ai.llmragwithspringai.rag.join.RRFDocumentJoiner;
import com.learning.ai.llmragwithspringai.rag.postretrieval.RelevanceDocumentReranker;
import com.learning.ai.llmragwithspringai.rag.postretrieval.RerankingDocumentRetriever;
import com.learning.ai.llmragwithspringai.rag.retrieval.CachingDocumentRetriever;
import com.learning.ai.llmragwithspringai.rag.retrieval.FilterContext;
import com.learning.ai.llmragwithspringai.rag.retrieval.HybridDocumentRetriever;
import com.learning.ai.llmragwithspringai.rag.retrieval.KeywordDocumentRetriever;
import com.learning.ai.llmragwithspringai.rag.splitter.SectionTextSplitter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.aop.ObservedAspect;
import java.util.concurrent.Executor;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.json.JsonMapper;

@Configuration(proxyBeanMethods = false)
public class AppConfig {

    private final RagChunkingProperties chunkingProperties;
    private final RagRetrievalProperties retrievalProperties;
    private final RagCacheProperties cacheProperties;

    public AppConfig(
            RagChunkingProperties chunkingProperties,
            RagRetrievalProperties retrievalProperties,
            RagCacheProperties cacheProperties) {
        this.chunkingProperties = chunkingProperties;
        this.retrievalProperties = retrievalProperties;
        this.cacheProperties = cacheProperties;
    }

    private DocumentRetriever applyDecorators(
            DocumentRetriever baseRetriever,
            boolean rerankEnabled,
            int rerankTopK,
            boolean cacheEnabled,
            CacheManager cacheManager,
            MeterRegistry meterRegistry) {
        DocumentRetriever retriever = baseRetriever;
        if (rerankEnabled) {
            retriever =
                    new RerankingDocumentRetriever(retriever, new RelevanceDocumentReranker(rerankTopK), meterRegistry);
        }
        if (cacheEnabled && cacheManager != null) {
            retriever = new CachingDocumentRetriever(retriever, cacheManager, meterRegistry);
        }
        return retriever;
    }

    @Bean
    ObservedAspect observedAspect(ObservationRegistry observationRegistry) {
        return new ObservedAspect(observationRegistry);
    }

    // Rationale: We use a default chunkSize of 300 to balance context richness and retrieval precision.
    // Nomic-embed-text supports a large context window, but chunks around 300-500 tokens generally yield the best RAG
    // results.
    // Note on Splitter Differences: Spring AI's TokenTextSplitter differs from LangChain4j's recursive splitter.
    // It does not have a direct 'token overlap' parameter; instead, it uses size constraints
    // and attempts to preserve semantic boundaries (like sentences).
    @Bean
    TextSplitter textSplitter() {

        TokenTextSplitter tokenSplitter = TokenTextSplitter.builder()
                .withChunkSize(chunkingProperties.getSize())
                .withMinChunkSizeChars(chunkingProperties.getMinSize())
                .build();

        if ("section".equalsIgnoreCase(chunkingProperties.getStrategy())) {
            return new SectionTextSplitter(chunkingProperties.getSectionPattern(), tokenSplitter);
        }
        return tokenSplitter;
    }

    @Bean
    @ConditionalOnProperty(name = "rag.retrieval.mode", havingValue = "keyword")
    DocumentRetriever keywordDocumentRetriever(
            org.springframework.beans.factory.ObjectProvider<CacheManager> cacheManagerProvider,
            JdbcTemplate jdbcTemplate,
            JsonMapper jsonMapper,
            MeterRegistry meterRegistry) {
        meterRegistry.counter("rag.retrieval.mode.active", "mode", "keyword").increment();
        return applyDecorators(
                new KeywordDocumentRetriever(
                        jdbcTemplate, retrievalProperties.getKeyword().getTopK(), jsonMapper),
                retrievalProperties.getRerank().isEnabled(),
                retrievalProperties.getRerank().getTopK(),
                cacheProperties.isEnabled(),
                cacheManagerProvider.getIfAvailable(),
                meterRegistry);
    }

    @Bean
    @ConditionalOnProperty(name = "rag.retrieval.mode", havingValue = "vector")
    DocumentRetriever vectorDocumentRetriever(
            VectorStore vectorStore,
            org.springframework.beans.factory.ObjectProvider<CacheManager> cacheManagerProvider,
            MeterRegistry meterRegistry) {
        meterRegistry.counter("rag.retrieval.mode.active", "mode", "vector").increment();

        DocumentRetriever baseRetriever = query -> {
            SearchRequest req = SearchRequest.builder()
                    .query(query.text())
                    .topK(retrievalProperties.getTopK())
                    .similarityThreshold(retrievalProperties.getSimilarityThreshold())
                    .build();
            if (FilterContext.getFilterExpression() != null) {
                req = SearchRequest.from(req)
                        .filterExpression(FilterContext.getFilterExpression())
                        .build();
            }
            return vectorStore.similaritySearch(req);
        };

        return applyDecorators(
                baseRetriever,
                retrievalProperties.getRerank().isEnabled(),
                retrievalProperties.getRerank().getTopK(),
                cacheProperties.isEnabled(),
                cacheManagerProvider.getIfAvailable(),
                meterRegistry);
    }

    @Bean
    @ConditionalOnProperty(name = "rag.retrieval.mode", havingValue = "hybrid", matchIfMissing = true)
    DocumentRetriever hybridDocumentRetriever(
            org.springframework.beans.factory.ObjectProvider<CacheManager> cacheManagerProvider,
            JdbcTemplate jdbcTemplate,
            JsonMapper jsonMapper,
            VectorStore vectorStore,
            Executor executor,
            MeterRegistry meterRegistry) {
        meterRegistry.counter("rag.retrieval.mode.active", "mode", "hybrid").increment();

        DocumentRetriever vectorRetriever = query -> {
            SearchRequest req = SearchRequest.builder()
                    .query(query.text())
                    .topK(retrievalProperties.getTopK())
                    .similarityThreshold(retrievalProperties.getSimilarityThreshold())
                    .build();
            if (FilterContext.getFilterExpression() != null) {
                req = SearchRequest.from(req)
                        .filterExpression(FilterContext.getFilterExpression())
                        .build();
            }
            return vectorStore.similaritySearch(req);
        };

        var keywordRetriever = new KeywordDocumentRetriever(
                jdbcTemplate, retrievalProperties.getKeyword().getTopK(), jsonMapper);
        var joiner = new RRFDocumentJoiner(
                retrievalProperties.getRrf().getK(),
                retrievalProperties.getHybrid().getTopK());

        return applyDecorators(
                new HybridDocumentRetriever(vectorRetriever, keywordRetriever, joiner, executor),
                retrievalProperties.getRerank().isEnabled(),
                retrievalProperties.getRerank().getTopK(),
                cacheProperties.isEnabled(),
                cacheManagerProvider.getIfAvailable(),
                meterRegistry);
    }
}
