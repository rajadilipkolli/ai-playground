package com.learning.ai.llmragwithspringai.service;

import com.learning.ai.llmragwithspringai.model.response.AIChatResponse;
import com.learning.ai.llmragwithspringai.model.response.RetrievalDiagnostic;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.annotation.Observed;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Service;

@Service
public class AIChatService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AIChatService.class);

    private final ChatClient aiClient;
    private final MeterRegistry meterRegistry;
    private final DocumentRetriever documentRetriever;
    private final List<ToolCallback> toolCallbacks;

    public AIChatService(
            ChatClient.Builder builder,
            MeterRegistry meterRegistry,
            DocumentRetriever documentRetriever,
            List<ToolCallback> toolCallbacks) {
        this.meterRegistry = meterRegistry;
        this.documentRetriever = documentRetriever;
        this.toolCallbacks = toolCallbacks;
        this.aiClient =
                builder.build(); // We will apply the advisor per request to use dynamic properties if needed, or we
        // can build it once.
    }

    @Observed(name = "rag.chat", contextualName = "rag-chat")
    public AIChatResponse chat(String query, boolean includeDiagnostics) {
        var queryAugmenter =
                ContextualQueryAugmenter.builder().allowEmptyContext(true).build();

        RetrievalAugmentationAdvisor advisor = RetrievalAugmentationAdvisor.builder()
                .documentRetriever(documentRetriever)
                .queryAugmenter(queryAugmenter)
                .build();

        var callResponse = aiClient.prompt()
                .system("""
                        You are a helpful customer support agent.
                        Answer the user's question using the provided context.
                        If the context does not contain the answer,
                        you may use your own knowledge ONLY if you are absolutely sure.
                        Otherwise, politely state that you do not have the answer.
                        """)
                .user(query)
                .advisors(advisor)
                .tools(toolCallbacks)
                .call();

        ChatResponse chatResponse = callResponse.chatResponse();
        String aiResponse = chatResponse.getResult().getOutput().getText();

        LOGGER.debug("Response received from call: {}", aiResponse);

        meterRegistry.counter("rag.llm.calls").increment();

        List<RetrievalDiagnostic> diagnostics = null;
        if (includeDiagnostics) {
            // Extract documents from context
            List<Document> docs = chatResponse.getMetadata().get(RetrievalAugmentationAdvisor.DOCUMENT_CONTEXT);
            if (docs == null) {
                docs = Collections.emptyList();
            }
            diagnostics = docs.stream()
                    .map(d -> {
                        Object vectorScore = d.getMetadata().get("distance");
                        Object keywordScore = d.getMetadata().get("ts_rank");
                        Object rrfScoreObj = d.getMetadata().get("rrf_score");
                        Object sourceObj = d.getMetadata().get("retrieval_source");

                        Double originalScore = 0.0;
                        if (vectorScore instanceof Number n) originalScore = n.doubleValue();
                        else if (keywordScore instanceof Number n) originalScore = n.doubleValue();

                        Double rrfScore = rrfScoreObj instanceof Number n ? n.doubleValue() : null;
                        String source = sourceObj instanceof String s ? s : "unknown";

                        LOGGER.debug(
                                "Retrieved document source: {}, rrfScore: {}, originalScore: {}",
                                source,
                                rrfScore,
                                originalScore);
                        return new RetrievalDiagnostic(d.getText(), originalScore, rrfScore, source);
                    })
                    .toList();
            meterRegistry.counter("rag.documents.retrieved").increment(docs.size());
            LOGGER.info("Retrieved {} documents for diagnostics", docs.size());
        }

        return new AIChatResponse(aiResponse, diagnostics);
    }
}
