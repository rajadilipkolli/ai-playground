package com.learning.ai.llmragwithspringai.evaluation;

import static org.assertj.core.api.Assertions.assertThat;

import com.learning.ai.llmragwithspringai.config.AbstractIntegrationTest;
import com.learning.ai.llmragwithspringai.model.response.AIChatResponse;
import com.learning.ai.llmragwithspringai.model.response.RetrievalDiagnostic;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.evaluation.RelevancyEvaluator;
import org.springframework.ai.document.Document;
import org.springframework.ai.evaluation.EvaluationRequest;
import org.springframework.ai.evaluation.EvaluationResponse;

class RagEvaluationIntTest extends AbstractIntegrationTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(RagEvaluationIntTest.class);

    private RelevancyEvaluator relevancyEvaluator;

    @BeforeEach
    void setUp() {
        this.relevancyEvaluator = new RelevancyEvaluator(chatClientBuilder);
    }

    static List<GoldenDatasetEntry> goldenDatasetProvider() throws Exception {
        return new GoldenDatasetLoader().loadDataset("golden-dataset.json");
    }

    @ParameterizedTest
    @MethodSource("goldenDatasetProvider")
    void testRagEvaluation(GoldenDatasetEntry entry) {
        LOGGER.info("Evaluating question: {}", entry.question());

        // 1. Invoke RAG Pipeline
        AIChatResponse chatResponse = aiChatService.chat(entry.question(), true);
        String responseText = chatResponse.queryResponse();

        List<RetrievalDiagnostic> diagnostics = chatResponse.diagnostics();
        List<String> contextList = diagnostics == null
                ? List.of()
                : diagnostics.stream().map(RetrievalDiagnostic::text).toList();

        List<Document> documentList = contextList.stream().map(Document::new).toList();
        EvaluationRequest evaluationRequest = new EvaluationRequest(entry.question(), documentList, responseText);
        EvaluationResponse evaluationResponse = relevancyEvaluator.evaluate(evaluationRequest);

        boolean isPass = evaluationResponse.isPass();
        LOGGER.info("Evaluation Pass: {}, Score: {}", isPass, evaluationResponse.getScore());

        // 2. Asserts
        assertThat(isPass).isTrue();

        assertThat(entry.expectedAnswerKeywords())
                .as("Response should contain at least one of the expected keywords")
                .anyMatch(keyword -> responseText.toLowerCase().contains(keyword.toLowerCase()));

        if (!entry.expectedContextKeywords().isEmpty()) {
            String fullContext = String.join(" ", contextList);
            for (String expectedContextKeyword : entry.expectedContextKeywords()) {
                assertThat(fullContext).containsIgnoringCase(expectedContextKeyword);
            }
        }
    }
}
