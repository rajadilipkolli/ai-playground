package com.learning.ai.llmragwithspringai.evaluation;

import static org.assertj.core.api.Assertions.*;

import com.learning.ai.llmragwithspringai.config.AbstractIntegrationTest;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * Integration test for evaluating RAG pipeline using the golden dataset.
 * Tests retrieve documents, generate responses, and validate both response content
 * and context relevance against expected keywords.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RagEvaluationIntTest extends AbstractIntegrationTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(RagEvaluationIntTest.class);

    @LocalServerPort
    private int localServerPort;

    @Autowired
    private VectorStore vectorStore;

    @Autowired
    private ChatClient.Builder chatClientBuilder;

    private ChatClient ragChatClient;
    private List<GoldenDatasetEntry> goldenDataset;

    @BeforeAll
    void setUp() throws IOException {
        // Initialize the RAG-enabled ChatClient with a vector store document retriever
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

        this.ragChatClient = chatClientBuilder
                .clone()
                .defaultAdvisors(retrievalAugmentationAdvisor)
                .build();

        // Load the golden dataset
        this.goldenDataset = GoldenDatasetLoader.loadGoldenDataset();

        LOGGER.info("RAG Evaluation test initialized with {} golden dataset entries", goldenDataset.size());
    }

    /**
     * Main evaluation test that iterates through the golden dataset,
     * executes the RAG pipeline, and validates responses against expected keywords.
     */
    @Test
    void testRagPipelineWithGoldenDataset() throws IOException {
        int totalEntries = goldenDataset.size();
        int passedEntries = 0;

        LOGGER.info("Starting evaluation of RAG pipeline against {} golden dataset entries", totalEntries);

        for (int i = 0; i < goldenDataset.size(); i++) {
            GoldenDatasetEntry entry = goldenDataset.get(i);
            LOGGER.info("Evaluating entry {} of {}: {}", i + 1, totalEntries, entry.question());

            try {
                // Execute RAG pipeline
                String response =
                        ragChatClient.prompt().user(entry.question()).call().content();

                // For simplicity, retrieve all documents to simulate context
                List<Document> retrievedDocuments = vectorStore.similaritySearch(entry.question());
                String context = retrievedDocuments.stream()
                        .map(Document::getText)
                        .reduce((a, b) -> a + "\n" + b)
                        .orElse("");

                LOGGER.debug("Generated response: {}", response);
                LOGGER.debug("Retrieved context length: {} characters", context.length());

                // Validate expected answer keywords appear in response
                for (String keyword : entry.expectedAnswerKeywords()) {
                    assertThat(response.toLowerCase())
                            .as("Response should contain keyword '" + keyword + "' for question: " + entry.question())
                            .contains(keyword.toLowerCase());
                }

                // Validate expected context keywords appear in retrieved documents
                for (String keyword : entry.expectedContextKeywords()) {
                    if (!keyword.isEmpty()) {
                        assertThat(context.toLowerCase())
                                .as("Retrieved context should contain keyword '" + keyword + "' for question: "
                                        + entry.question())
                                .contains(keyword.toLowerCase());
                    }
                }

                passedEntries++;
                LOGGER.info("Entry {} evaluation PASSED", i + 1);

            } catch (AssertionError e) {
                LOGGER.error("Entry {} evaluation FAILED: {}", i + 1, e.getMessage());
                throw e;
            } catch (Exception e) {
                LOGGER.error("Entry {} evaluation encountered exception: {}", i + 1, e.getMessage(), e);
                throw new AssertionError("Failed to evaluate entry " + (i + 1) + ": " + e.getMessage(), e);
            }
        }

        // Log overall results
        double passRate = (double) passedEntries / totalEntries * 100;

        LOGGER.info("=== RAG Evaluation Summary ===");
        LOGGER.info("Total Entries Evaluated: {}", totalEntries);
        LOGGER.info("Entries Passed: {} ({:.1f}%)", passedEntries, passRate);
        LOGGER.info("=============================");

        // Assert that all entries passed
        assertThat(passedEntries)
                .as("All golden dataset entries should pass evaluation")
                .isEqualTo(totalEntries);
    }
}
