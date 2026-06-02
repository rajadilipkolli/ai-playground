package com.learning.ai.llmragwithspringai.evaluation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.learning.ai.llmragwithspringai.config.AbstractIntegrationTest;
import com.learning.ai.llmragwithspringai.model.response.AIChatResponse;
import com.learning.ai.llmragwithspringai.model.response.RetrievalDiagnostic;
import com.learning.ai.llmragwithspringai.service.AIChatService;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.evaluation.RelevancyEvaluator;
import org.springframework.ai.document.Document;
import org.springframework.ai.evaluation.EvaluationRequest;
import org.springframework.ai.evaluation.EvaluationResponse;
import org.springframework.beans.factory.annotation.Autowired;

public class RagEvaluationIntTest extends AbstractIntegrationTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(RagEvaluationIntTest.class);

    @Autowired
    private ChatClient.Builder chatClientBuilder;

    @Autowired
    private AIChatService aiChatService;

    private RelevancyEvaluator relevancyEvaluator;
    private GoldenDatasetLoader datasetLoader;

    @BeforeEach
    void setUp() {
        this.relevancyEvaluator = new RelevancyEvaluator(chatClientBuilder);
        this.datasetLoader = new GoldenDatasetLoader();
    }

    @Test
    void testRagEvaluation() throws Exception {
        List<GoldenDatasetEntry> dataset = datasetLoader.loadDataset("golden-dataset.json");
        assertThat(dataset).isNotEmpty();

        int passedEvaluations = 0;
        int totalEvaluations = dataset.size();

        for (GoldenDatasetEntry entry : dataset) {
            LOGGER.info("Evaluating question: {}", entry.question());

            // 1. Invoke RAG Pipeline
            AIChatResponse chatResponse = aiChatService.chat(entry.question(), true);
            String responseText = chatResponse.queryResponse();
            List<String> contextList = chatResponse.diagnostics().stream()
                    .map(RetrievalDiagnostic::text)
                    .collect(Collectors.toList());

            List<Document> documentList =
                    contextList.stream().map(Document::new).collect(Collectors.toList());
            EvaluationRequest evaluationRequest = new EvaluationRequest(entry.question(), documentList, responseText);
            EvaluationResponse evaluationResponse = relevancyEvaluator.evaluate(evaluationRequest);

            boolean isPass = evaluationResponse.isPass();
            LOGGER.info("Evaluation Pass: {}, Score: {}", isPass, evaluationResponse.getScore());

            if (isPass) {
                passedEvaluations++;
            }

            // 3. Asserts
            assertTrue(isPass, "The LLM evaluator determined the response was not relevant.");

            for (String expectedKeyword : entry.expectedAnswerKeywords()) {
                assertThat(responseText).containsIgnoringCase(expectedKeyword);
            }

            if (!entry.expectedContextKeywords().isEmpty()) {
                String fullContext = String.join(" ", contextList);
                for (String expectedContextKeyword : entry.expectedContextKeywords()) {
                    assertThat(fullContext).containsIgnoringCase(expectedContextKeyword);
                }
            }
        }

        LOGGER.info("Evaluation Complete. Pass Rate: {}/{}", passedEvaluations, totalEvaluations);
    }
}
