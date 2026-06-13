package com.learning.ai.llmragwithspringai.evaluation;

import static org.assertj.core.api.Assertions.assertThat;

import com.learning.ai.llmragwithspringai.config.AbstractIntegrationTest;
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
import org.springframework.ai.evaluation.EvaluationRequest;
import org.springframework.ai.evaluation.EvaluationResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@org.junit.jupiter.api.Disabled("Flaky evaluation test due to LLM response variability")
class RagEvaluationIntTest extends AbstractIntegrationTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(RagEvaluationIntTest.class);

    private RelevancyEvaluator relevancyEvaluator;

    @Value("classpath:Rohit_Gurunath_Sharma.pdf")
    private Resource pdfResource;

    @BeforeAll
    void setUp() {
        if (dataIndexerService.isEmpty()) {
            dataIndexerService.loadData(pdfResource, null, null, null);
        }
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
        com.learning.ai.llmragwithspringai.model.request.AIChatRequest req =
                new com.learning.ai.llmragwithspringai.model.request.AIChatRequest(entry.question(), null, null, null);
        AIChatResponse chatResponse = aiChatService.chat(req, true);
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
        // The RelevancyEvaluator will return true if the response aligns with the context.
        if (!entry.expectedContextKeywords().isEmpty()) {
            assertThat(isPass)
                    .as("Relevancy evaluation should pass (response is grounded in the provided context)")
                    .isTrue();
        }

        assertThat(entry.expectedAnswerKeywords())
                .as("Response should contain at least one of the expected keywords")
                .anyMatch(keyword -> responseText.toLowerCase().contains(keyword.toLowerCase()));

        if (!entry.expectedContextKeywords().isEmpty()) {
            String fullContext = String.join(" ", contextList).replaceAll("\\s+", " ");
            assertThat(entry.expectedContextKeywords())
                    .as("Context should contain at least one of the expected keywords")
                    .anyMatch(keyword -> fullContext.toLowerCase().contains(keyword.toLowerCase()));
        }
    }
}
