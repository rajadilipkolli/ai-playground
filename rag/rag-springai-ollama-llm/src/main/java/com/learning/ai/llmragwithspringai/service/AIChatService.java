package com.learning.ai.llmragwithspringai.service;

import com.learning.ai.llmragwithspringai.model.response.AIChatResponse;
import com.learning.ai.llmragwithspringai.model.response.RetrievalDiagnostic;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

@Service
public class AIChatService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AIChatService.class);

    private final ChatClient aiClient;
    private final VectorStore vectorStore;
    private final MeterRegistry meterRegistry;

    @Value("${rag.retrieval.topK:3}")
    private int topK;

    @Value("${rag.retrieval.similarityThreshold:0.6}")
    private double similarityThreshold;

    public AIChatService(
            ChatClient.Builder builder,
            VectorStore vectorStore,
            io.micrometer.core.instrument.MeterRegistry meterRegistry) {
        this.vectorStore = vectorStore;
        this.meterRegistry = meterRegistry;
        this.aiClient =
                builder.build(); // We will apply the advisor per request to use dynamic properties if needed, or we
        // can build it once.
    }

    public AIChatResponse chat(String query, boolean includeDiagnostics) {
        StopWatch stopWatch = new StopWatch("chat");
        stopWatch.start();

        var documentRetriever = VectorStoreDocumentRetriever.builder()
                .vectorStore(vectorStore)
                .topK(topK)
                .similarityThreshold(similarityThreshold)
                .build();

        var queryAugmenter =
                ContextualQueryAugmenter.builder().allowEmptyContext(true).build();

        RetrievalAugmentationAdvisor advisor = RetrievalAugmentationAdvisor.builder()
                .documentRetriever(documentRetriever)
                .queryAugmenter(queryAugmenter)
                .build();

        String aiResponse = aiClient.prompt()
                .system(
                        "You are a helpful customer support agent. Use the provided information segments to synthesize your answer. If the segments do not contain relevant information, politely state that you do not have the answer.")
                .user(query)
                .advisors(advisor)
                .call()
                .content();

        stopWatch.stop();
        LOGGER.info("Response received from call in {} ms: {}", stopWatch.getTotalTimeMillis(), aiResponse);
        meterRegistry.timer("rag.retrieval.latency").record(Duration.ofMillis(stopWatch.getTotalTimeMillis()));
        meterRegistry.counter("rag.llm.calls").increment();

        List<RetrievalDiagnostic> diagnostics = null;
        if (includeDiagnostics) {
            List<Document> docs = documentRetriever.retrieve(new Query(query));
            diagnostics = docs.stream()
                    .map(d -> {
                        Object score = d.getMetadata().getOrDefault("distance", 0.0);
                        LOGGER.debug("Retrieved document score: {}", score);
                        return new RetrievalDiagnostic(d.getText(), score);
                    })
                    .toList();
            meterRegistry.counter("rag.documents.retrieved").increment(docs.size());
            LOGGER.info("Retrieved {} documents for diagnostics", docs.size());
        }

        return new AIChatResponse(aiResponse, diagnostics);
    }
}
