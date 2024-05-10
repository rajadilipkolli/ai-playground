package com.learning.ai.service;

import com.learning.ai.domain.response.AICustomerSupportResponse;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.springframework.stereotype.Service;

@Service
public class CustomerSupportService {

    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final AICustomerSupportAgent aiCustomerSupportAgent;

    public CustomerSupportService(
            EmbeddingModel embeddingModel,
            EmbeddingStore<TextSegment> embeddingStore,
            AICustomerSupportAgent aiCustomerSupportAgent) {
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
        this.aiCustomerSupportAgent = aiCustomerSupportAgent;
    }

    public AICustomerSupportResponse chat(String question) {

        Embedding queryEmbedding = embeddingModel.embed(question).content();
        EmbeddingSearchRequest embeddingSearchRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(1)
                .build();
        EmbeddingSearchResult<TextSegment> relevant = embeddingStore.search(embeddingSearchRequest);
        EmbeddingMatch<TextSegment> embeddingMatch = relevant.matches().get(0);

        String embeddedText = embeddingMatch.embedded().text();
        return aiCustomerSupportAgent.chat(question, embeddedText);
    }
}
