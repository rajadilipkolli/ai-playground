package com.learning.ai.llmragwithspringai.evaluation;

import static org.assertj.core.api.Assertions.assertThat;

import com.learning.ai.llmragwithspringai.config.AbstractIntegrationTest;
import com.learning.ai.llmragwithspringai.model.request.AIChatRequest;
import com.learning.ai.llmragwithspringai.model.response.AIChatResponse;
import com.learning.ai.llmragwithspringai.model.response.RetrievalDiagnostic;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.evaluation.RelevancyEvaluator;
import org.springframework.ai.document.Document;
import org.springframework.ai.evaluation.EvaluationResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RAGASEvaluationIntTest extends AbstractIntegrationTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(RAGASEvaluationIntTest.class);

    private RelevancyEvaluator relevancyEvaluator;
    private FaithfulnessEvaluator faithfulnessEvaluator;
    private ContextPrecisionEvaluator contextPrecisionEvaluator;
    private ContextRecallEvaluator contextRecallEvaluator;

    @Value("classpath:Rohit_Gurunath_Sharma.pdf")
    private Resource pdfResource;

    @BeforeAll
    void setUp() {
        if (dataIndexerService.isEmpty()) {
            dataIndexerService.loadData(pdfResource, "profile", "cricket_board", "sports");
        }
        this.relevancyEvaluator = new RelevancyEvaluator(chatClientBuilder);
        this.faithfulnessEvaluator = new FaithfulnessEvaluator(chatClientBuilder);
        this.contextPrecisionEvaluator = new ContextPrecisionEvaluator(chatClientBuilder);
        this.contextRecallEvaluator = new ContextRecallEvaluator(chatClientBuilder);
    }

    static List<GoldenDatasetEntry> goldenDatasetProvider() throws Exception {
        return new GoldenDatasetLoader().loadDataset("golden-dataset.json");
    }

    @ParameterizedTest
    @MethodSource("goldenDatasetProvider")
    void testRagasMetrics(GoldenDatasetEntry entry) {
        LOGGER.info("Evaluating question: {}", entry.question());

        // 1. Invoke RAG Pipeline
        AIChatResponse chatResponse = aiChatService.chat(
                new AIChatRequest(entry.question(), "profile", "cricket_board", "sports", null), true);
        String responseText = chatResponse.queryResponse();

        List<RetrievalDiagnostic> diagnostics = chatResponse.diagnostics();
        List<String> contextList = diagnostics == null
                ? List.of()
                : diagnostics.stream().map(RetrievalDiagnostic::text).toList();

        List<Document> documentList = contextList.stream().map(Document::new).toList();
        RagasEvaluationRequest evaluationRequest =
                new RagasEvaluationRequest(entry.question(), documentList, responseText, entry.groundTruthAnswer());

        // 2. Evaluate
        EvaluationResponse relevancyResponse = relevancyEvaluator.evaluate(evaluationRequest);
        EvaluationResponse faithfulnessResponse = faithfulnessEvaluator.evaluate(evaluationRequest);
        EvaluationResponse contextPrecisionResponse = contextPrecisionEvaluator.evaluate(evaluationRequest);
        EvaluationResponse contextRecallResponse = contextRecallEvaluator.evaluate(evaluationRequest);

        // 3. Asserts
        if (!entry.expectedContextKeywords().isEmpty()) {
            // Factual questions
            assertThat(relevancyResponse.isPass())
                    .as("Relevancy evaluation should pass")
                    .isTrue();

            assertThat(faithfulnessResponse.isPass())
                    .as("Faithfulness evaluation should pass (Score: " + faithfulnessResponse.getScore() + ")")
                    .isTrue();

            assertThat(contextPrecisionResponse.isPass())
                    .as("Context Precision evaluation should pass (Score: " + contextPrecisionResponse.getScore() + ")")
                    .isTrue();
        }

        if (entry.groundTruthAnswer() != null && !entry.groundTruthAnswer().isBlank()) {
            assertThat(contextRecallResponse.isPass())
                    .as("Context Recall evaluation should pass (Score: " + contextRecallResponse.getScore() + ")")
                    .isTrue();
        } else {
            assertThat(contextRecallResponse.getScore())
                    .as("Context Recall score should be -1.0 for missing ground truth")
                    .isEqualTo(-1.0f);
        }
    }
}
