package com.learning.ai.service;

import com.learning.ai.domain.response.AICustomerSupportResponse;
import com.learning.ai.domain.response.AICustomerSupportResponseWrapper;
import com.learning.ai.domain.response.RetrievalDiagnostic;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
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

    private final io.micrometer.core.instrument.MeterRegistry meterRegistry;

    public CustomerSupportService(
            EmbeddingModel embeddingModel,
            EmbeddingStore<TextSegment> embeddingStore,
            AICustomerSupportAgent aiCustomerSupportAgent,
            io.micrometer.core.instrument.MeterRegistry meterRegistry) {
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
        this.aiCustomerSupportAgent = aiCustomerSupportAgent;
        this.meterRegistry = meterRegistry;
    }

    public AICustomerSupportResponseWrapper chat(String question, boolean includeDiagnostics) {
        try (var mdc = MDC.putCloseable("queryId", UUID.randomUUID().toString())) {
            log.info("Processing question: {}", question);
            long startTime = System.currentTimeMillis();

            Embedding queryEmbedding = embeddingModel.embed(question).content();
            long embeddingDuration = System.currentTimeMillis() - startTime;
            log.debug("Embedding generated in {} ms", embeddingDuration);
            meterRegistry.timer("rag.embedding.latency").record(Duration.ofMillis(embeddingDuration));
            meterRegistry.counter("rag.embedding.requests").increment();

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
            meterRegistry.timer("rag.search.latency").record(Duration.ofMillis(searchDuration));
            meterRegistry.counter("rag.search.requests").increment();

            List<RetrievalDiagnostic> diagnostics = new ArrayList<>();

            if (matches.isEmpty()) {
                log.warn("No matches found above minScore {}", minScore);
                meterRegistry.counter("rag.agent.calls").increment();
                AICustomerSupportResponse response =
                        aiCustomerSupportAgent.chat(question, "No relevant information found in the knowledge base.");
                return new AICustomerSupportResponseWrapper(response, includeDiagnostics ? diagnostics : null);
            }

            StringBuilder contextBuilder = new StringBuilder();
            for (int i = 0; i < matches.size(); i++) {
                EmbeddingMatch<TextSegment> match = matches.get(i);
                String text = match.embedded().text();
                double score = match.score();
                log.debug("Match {} score: {}", i + 1, score);

                if (includeDiagnostics) {
                    diagnostics.add(new RetrievalDiagnostic(text, score));
                }

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
            meterRegistry.counter("rag.agent.calls").increment();
            AICustomerSupportResponse response = aiCustomerSupportAgent.chat(question, embeddedText);
            return new AICustomerSupportResponseWrapper(response, includeDiagnostics ? diagnostics : null);
        }
    }
}
