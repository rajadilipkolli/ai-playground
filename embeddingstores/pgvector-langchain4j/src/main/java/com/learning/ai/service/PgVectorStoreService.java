package com.learning.ai.service;

import com.learning.ai.domain.response.AIChatResponse;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PgVectorStoreService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PgVectorStoreService.class);

    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;

    public PgVectorStoreService(EmbeddingModel embeddingModel, EmbeddingStore<TextSegment> embeddingStore) {
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
    }

    public AIChatResponse queryEmbeddingStore(String question, Integer userId) {
        Embedding queryEmbedding = embeddingModel.embed(question).content();
        EmbeddingSearchRequest.EmbeddingSearchRequestBuilder embeddingSearchRequestBuilder =
                EmbeddingSearchRequest.builder().queryEmbedding(queryEmbedding).maxResults(1);
        if (userId != null) {
            Filter equalTo = MetadataFilterBuilder.metadataKey("userId").isEqualTo(userId);
            embeddingSearchRequestBuilder.filter(equalTo);
        }
        EmbeddingSearchRequest embeddingSearchRequest = embeddingSearchRequestBuilder.build();
        EmbeddingSearchResult<TextSegment> relevant = embeddingStore.search(embeddingSearchRequest);
        EmbeddingMatch<TextSegment> embeddingMatch = relevant.matches().get(0);

        LOGGER.info("Score : {}", embeddingMatch.score()); // 0.8144288608390052
        String answer = embeddingMatch.embedded().text();
        LOGGER.info("Embedded Segment : {}", answer); // I like football.
        return new AIChatResponse(answer);
    }
}
