package com.learning.ai.service;

import com.learning.ai.domain.response.AIChatResponse;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingStore;
import java.util.List;
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

    public AIChatResponse queryEmbeddingStore(String question) {
        Embedding queryEmbedding = embeddingModel.embed(question).content();
        List<EmbeddingMatch<TextSegment>> relevant = embeddingStore.findRelevant(queryEmbedding, 1);
        EmbeddingMatch<TextSegment> embeddingMatch = relevant.get(0);

        LOGGER.info("Score : {}", embeddingMatch.score()); // 0.8144288608390052
        LOGGER.info("Embedded Segment : {}", embeddingMatch.embedded());
        // I like football.
        return new AIChatResponse(embeddingMatch.embedded().text());
    }
}
