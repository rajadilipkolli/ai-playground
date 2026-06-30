package com.learning.ai.llmragwithspringai.service;

import com.learning.ai.llmragwithspringai.config.properties.GuardrailsProperties;
import com.learning.ai.llmragwithspringai.config.properties.RagQueryProperties;
import com.learning.ai.llmragwithspringai.model.request.AIChatRequest;
import com.learning.ai.llmragwithspringai.model.response.AIChatResponse;
import com.learning.ai.llmragwithspringai.model.response.RetrievalDiagnostic;
import com.learning.ai.llmragwithspringai.rag.query.QueryAnalysisResult;
import com.learning.ai.llmragwithspringai.rag.query.QueryAnalyzer;
import com.learning.ai.llmragwithspringai.rag.retrieval.FilterContext;
import com.learning.ai.llmragwithspringai.util.FilterExpressionBuilderUtil;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.annotation.Observed;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SafeGuardAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.preretrieval.query.expansion.QueryExpander;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
@SuppressWarnings("NullAway")
public class AIChatService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AIChatService.class);

    private final ChatClient aiClient;
    private final MeterRegistry meterRegistry;
    private final DocumentRetriever documentRetriever;
    private final List<ToolCallback> toolCallbacks;
    private final GuardrailsProperties guardrailsProperties;
    private final Optional<QueryExpander> queryExpander;
    private final Optional<QueryAnalyzer> queryAnalyzer;
    private final RagQueryProperties ragQueryProperties;
    private final Resource reactSystemPrompt;

    public AIChatService(
            ChatClient.Builder builder,
            MeterRegistry meterRegistry,
            DocumentRetriever documentRetriever,
            List<ToolCallback> toolCallbacks,
            GuardrailsProperties guardrailsProperties,
            Optional<QueryExpander> queryExpander,
            Optional<QueryAnalyzer> queryAnalyzer,
            RagQueryProperties ragQueryProperties,
            @Value("classpath:prompts/react-system-prompt.txt") Resource reactSystemPrompt) {
        this.meterRegistry = meterRegistry;
        this.documentRetriever = documentRetriever;
        this.toolCallbacks = toolCallbacks;
        this.guardrailsProperties = guardrailsProperties;
        this.queryExpander = queryExpander;
        this.queryAnalyzer = queryAnalyzer;
        this.ragQueryProperties = ragQueryProperties;
        this.reactSystemPrompt = reactSystemPrompt;
        this.aiClient =
                builder.build(); // We will apply the advisor per request to use dynamic properties if needed, or we
        // can build it once.
    }

    @Observed(name = "rag.chat", contextualName = "rag-chat")
    public AIChatResponse chat(AIChatRequest request, boolean includeDiagnostics) {
        Map<String, Object> explicitFilters = getExplicitFilters(request);
        Map<String, Object> mergedFilters = new HashMap<>();

        String finalQuestion = request.question();

        if (ragQueryProperties != null && ragQueryProperties.isSelfQueryingEnabled() && queryAnalyzer.isPresent()) {
            QueryAnalysisResult analysisResult = queryAnalyzer.get().analyze(request.question());
            if (analysisResult != null) {
                if (analysisResult.cleanedQuery() != null
                        && !analysisResult.cleanedQuery().isBlank()) {
                    finalQuestion = analysisResult.cleanedQuery();
                }
                if (analysisResult.filters() != null) {
                    mergedFilters.putAll(analysisResult.filters());
                }
            }
        }

        mergedFilters.putAll(explicitFilters);

        Filter.Expression filterExpression = FilterExpressionBuilderUtil.build(mergedFilters);
        final String effectiveQuestion = finalQuestion;

        try {
            FilterContext.clearRetrievedDocuments();
            return ScopedValue.where(FilterContext.FILTER_EXPRESSION, filterExpression)
                    .call(() -> {
                        var queryAugmenter = ContextualQueryAugmenter.builder()
                                .allowEmptyContext(true)
                                .build();

                        var advisorBuilder = RetrievalAugmentationAdvisor.builder()
                                .documentRetriever(documentRetriever)
                                .queryAugmenter(queryAugmenter);

                        queryExpander.ifPresent(advisorBuilder::queryExpander);

                        RetrievalAugmentationAdvisor advisor = advisorBuilder.build();

                        List<Advisor> advisors = new ArrayList<>();
                        advisors.add(advisor);
                        if (guardrailsProperties.getLogging().isEnabled()) {
                            advisors.add(new SimpleLoggerAdvisor());
                        }
                        if (guardrailsProperties.getSensitiveWords() != null
                                && !guardrailsProperties.getSensitiveWords().isEmpty()) {
                            List<String> words = guardrailsProperties.getSensitiveWords().stream()
                                    .flatMap(w -> Arrays.stream(w.split(",")))
                                    .map(String::trim)
                                    .toList();
                            advisors.add(new SafeGuardAdvisor(
                                    words, guardrailsProperties.getFailureMessage(), Ordered.HIGHEST_PRECEDENCE));
                        }

                        ChatClient.ChatClientRequestSpec callRequestSpec = aiClient.prompt()
                                .system(reactSystemPrompt)
                                .user(effectiveQuestion)
                                .advisors(advisors)
                                .tools(toolCallbacks);

                        ChatClient.CallResponseSpec callResponse;
                        try {
                            callResponse = callRequestSpec.call();
                        } catch (IllegalArgumentException e) {
                            if (guardrailsProperties.getFailureMessage() != null
                                    && e.getMessage() != null
                                    && e.getMessage().contains(guardrailsProperties.getFailureMessage())) {
                                return new AIChatResponse(guardrailsProperties.getFailureMessage(), null);
                            }
                            throw e;
                        }

                        ChatResponse chatResponse = callResponse.chatResponse();
                        String aiResponse = "I'm sorry, I was unable to generate a response. Please try again.";
                        if (chatResponse != null
                                && chatResponse.getResult() != null
                                && chatResponse.getResult().getOutput().getText() != null) {
                            aiResponse = chatResponse.getResult().getOutput().getText();
                        }

                        LOGGER.debug("Response received from call: {}", aiResponse);

                        meterRegistry.counter("rag.llm.calls").increment();

                        List<RetrievalDiagnostic> diagnostics = null;
                        if (includeDiagnostics && chatResponse != null) {
                            List<Document> docs = new ArrayList<>();

                            // 1. Get docs from Advisor (if it ran)
                            List<Document> advisorDocs =
                                    chatResponse.getMetadata().get(RetrievalAugmentationAdvisor.DOCUMENT_CONTEXT);
                            if (advisorDocs != null) {
                                docs.addAll(advisorDocs);
                            }

                            // 2. Get docs from Tool (if it ran)
                            List<Document> toolDocs = FilterContext.getRetrievedDocuments();
                            if (toolDocs != null) {
                                for (Document d : toolDocs) {
                                    if (!docs.contains(d)) {
                                        docs.add(d);
                                    }
                                }
                            }
                            diagnostics = docs.stream()
                                    .map(d -> {
                                        Object vectorScore = d.getMetadata().get("distance");
                                        Object keywordScore = d.getMetadata().get("ts_rank");
                                        Object rrfScoreObj = d.getMetadata().get("rrf_score");
                                        Object sourceObj = d.getMetadata().get("retrieval_source");

                                        double originalScore = 0.0;
                                        if (vectorScore instanceof Number n) originalScore = n.doubleValue();
                                        else if (keywordScore instanceof Number n) originalScore = n.doubleValue();

                                        String text = d.getText() != null ? d.getText() : "";
                                        Double rrfScore = rrfScoreObj instanceof Number n ? n.doubleValue() : 0.0;
                                        String source = sourceObj instanceof String s ? s : "unknown";

                                        LOGGER.debug(
                                                "Retrieved document source: {}, rrfScore: {}, originalScore: {}",
                                                source,
                                                rrfScore,
                                                originalScore);
                                        return new RetrievalDiagnostic(text, originalScore, rrfScore, source);
                                    })
                                    .toList();
                            meterRegistry.counter("rag.documents.retrieved").increment(docs.size());
                            LOGGER.info("Retrieved {} documents for diagnostics", docs.size());
                        }

                        return new AIChatResponse(aiResponse, diagnostics);
                    });
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        } finally {
            FilterContext.clearRetrievedDocuments();
        }
    }

    private static @NonNull Map<String, Object> getExplicitFilters(AIChatRequest request) {
        Map<String, Object> filters = new HashMap<>();
        String documentType =
                request.documentType() == null ? null : request.documentType().trim();
        if (documentType != null && !documentType.isEmpty()) {
            filters.put("documentType", documentType);
        }

        String owner = request.owner() == null ? null : request.owner().trim();
        if (owner != null && !owner.isEmpty()) {
            filters.put("owner", owner);
        }

        String category = request.category() == null ? null : request.category().trim();
        if (category != null && !category.isEmpty()) {
            filters.put("category", category);
        }

        if (request.filters() != null) {
            filters.putAll(request.filters());
        }
        return filters;
    }
}
