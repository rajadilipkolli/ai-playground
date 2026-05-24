package com.learning.ai.llmragwithspringai.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

@Service
public class AIChatService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AIChatService.class);
    private static final double RELEVANCE_THRESHOLD = 0.70;

    private final ChatClient aiClient;
    private final MeterRegistry meterRegistry;
    private final Timer retrievalLatencyTimer;
    private final Counter retrievalDocumentsHighCounter;
    private final Counter retrievalDocumentsLowCounter;
    private final DistributionSummary contextLengthSummary;

    public AIChatService(ChatClient.Builder builder, VectorStore vectorStore, MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        var documentRetriever = VectorStoreDocumentRetriever.builder()
                .vectorStore(vectorStore)
                .similarityThreshold(0.50)
                .build();

        var queryAugmenter =
                ContextualQueryAugmenter.builder().allowEmptyContext(true).build();

        RetrievalAugmentationAdvisor retrievalAugmentationAdvisor = RetrievalAugmentationAdvisor.builder()
                .documentRetriever(documentRetriever)
                .queryAugmenter(queryAugmenter)
                .build();
        this.aiClient =
                builder.clone().defaultAdvisors(retrievalAugmentationAdvisor).build();

        // Register custom metrics
        this.retrievalLatencyTimer = Timer.builder("rag.retrieval.latency")
                .description("Measures document retrieval time, separate from LLM latency")
                .register(meterRegistry);

        this.retrievalDocumentsHighCounter = Counter.builder("rag.retrieval.documents")
                .tag("relevance", "high")
                .description("Number of high-relevance documents retrieved")
                .register(meterRegistry);

        this.retrievalDocumentsLowCounter = Counter.builder("rag.retrieval.documents")
                .tag("relevance", "low")
                .description("Number of low-relevance documents retrieved")
                .register(meterRegistry);

        this.contextLengthSummary = DistributionSummary.builder("rag.context.length")
                .description("Tracks token count of retrieved context")
                .register(meterRegistry);
    }

    @Observed(name = "rag.chat", contextualName = "rag-chat")
    public String chat(String query) {
        String aiResponse = aiClient.prompt().user(query).call().content();
        LOGGER.info("Response received from call :{}", aiResponse);
        return aiResponse;
    }
}
