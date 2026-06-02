package com.learning.ai.service;

import com.learning.ai.domain.response.AICustomerSupportResponse;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class CustomerSupportService {

    private static final Logger log = LoggerFactory.getLogger(CustomerSupportService.class);

    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final AICustomerSupportAgent aiCustomerSupportAgent;

    @Value("${langchain4j.rag.retrieval.maxResults:3}")
    private int maxResults;

    @Value("${langchain4j.rag.retrieval.minScore:0.6}")
    private double minScore;

    public CustomerSupportService(
            EmbeddingModel embeddingModel,
            EmbeddingStore<TextSegment> embeddingStore,
            AICustomerSupportAgent aiCustomerSupportAgent) {
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
        this.aiCustomerSupportAgent = aiCustomerSupportAgent;
    }

    public AICustomerSupportResponse chat(String question) {
        log.info("Processing question: {}", question);
        long startTime = System.currentTimeMillis();

        Embedding queryEmbedding = embeddingModel.embed(question).content();

        log.debug("Embedding generated in {} ms", System.currentTimeMillis() - startTime);

        EmbeddingSearchRequest embeddingSearchRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(maxResults)
                .minScore(minScore)
                .build();

        long searchStartTime = System.currentTimeMillis();
        EmbeddingSearchResult<TextSegment> relevant = embeddingStore.search(embeddingSearchRequest);
        long searchDuration = System.currentTimeMillis() - searchStartTime;

        List<EmbeddingMatch<TextSegment>> matches = relevant.matches();
        log.info("Found {} matches in {} ms", matches.size(), searchDuration);

        if (matches.isEmpty()) {
            log.warn("No matches found above minScore {}", minScore);
            return aiCustomerSupportAgent.chat(question, "No relevant information found in the knowledge base.");
        }

        StringBuilder contextBuilder = new StringBuilder();
        for (int i = 0; i < matches.size(); i++) {
            EmbeddingMatch<TextSegment> match = matches.get(i);
            String text = match.embedded().text();
            double score = match.score();

            log.info("Match {} score: {}", i + 1, score);

            contextBuilder
                    .append("--- Section ")
                    .append(i + 1)
                    .append(" (Score: ")
                    .append(score)
                    .append(") ---\n")
                    .append(text)
                    .append("\n\n");
        }

        String embeddedText = contextBuilder.toString().trim();
        return aiCustomerSupportAgent.chat(question, embeddedText);
    }
}
