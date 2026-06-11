package com.learning.ai.reactrag;

import static org.assertj.core.api.Assertions.assertThat;

import com.learning.ai.reactrag.common.ContainerConfig;
import com.learning.ai.reactrag.model.request.ChatRequest;
import com.learning.ai.reactrag.model.response.ChatResponse;
import com.learning.ai.reactrag.service.DocumentIngestionService;
import com.learning.ai.reactrag.util.ContentHashUtil;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = ContainerConfig.class)
@AutoConfigureRestTestClient
class ReactRagApplicationTests {

    @LocalServerPort
    private int port;

    @Autowired
    private RestClient.Builder restClientBuilder;

    @Autowired
    private VectorStore vectorStore;

    @Autowired
    private DocumentIngestionService documentIngestionService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String currentTestId;

    @AfterEach
    void tearDown() {
        if (currentTestId != null) {
            List<String> ids = jdbcTemplate.queryForList(
                    "SELECT id FROM vector_store WHERE metadata->>'testId' = ?", String.class, currentTestId);
            if (!ids.isEmpty()) {
                vectorStore.delete(ids);
                currentTestId = null;
            }
        }
    }

    @Test
    void testChatEndpoint() {
        RestClient restClient =
                restClientBuilder.baseUrl("http://localhost:" + port).build();
        ChatRequest request = new ChatRequest("What is 10 plus 15?");
        ResponseEntity<ChatResponse> response =
                restClient.post().uri("/api/chat").body(request).retrieve().toEntity(ChatResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().answer()).isNotBlank();
        assertThat(response.getBody().answer()).contains("25");
    }

    @Test
    void testKnowledgeSearchTool() {
        currentTestId = UUID.randomUUID().toString();
        vectorStore.accept(List.of(new Document(
                "The company's secret project is named Project Antigravity.", Map.of("testId", currentTestId))));

        RestClient restClient =
                restClientBuilder.baseUrl("http://localhost:" + port).build();
        ChatRequest request = new ChatRequest(
                "Search the internal knowledge base for the name of the company's secret project. Answer with the project name.");
        ResponseEntity<ChatResponse> response =
                restClient.post().uri("/api/chat").body(request).retrieve().toEntity(ChatResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().answer()).isNotBlank();
        assertThat(response.getBody().answer()).contains("Antigravity");
    }

    @Test
    void testCalculatorToolWithRceAttempt() {
        RestClient restClient =
                restClientBuilder.baseUrl("http://localhost:" + port).build();
        // SpEL RCE payload: T(java.lang.Runtime).getRuntime().exec("calc")
        // With exp4j, this should fail to parse and return an error from the tool,
        // which the LLM will then relay back or gracefully handle without executing anything.
        ChatRequest request = new ChatRequest(
                "Calculate the following exact mathematical expression: T(java.lang.Runtime).getRuntime().exec(\"calc\")");
        ResponseEntity<ChatResponse> response =
                restClient.post().uri("/api/chat").body(request).retrieve().toEntity(ChatResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().answer()).isNotBlank();
        // Since exp4j throws an exception for non-math strings, the LLM will see the tool's error
        // or fail to parse it gracefully without server compromise.
    }

    @Test
    void testDocumentIngestionDuplicateSkip() {
        MultipartFile file = new MockMultipartFile(
                "file",
                "test-duplicate.txt",
                "text/plain",
                "This is a test document to check duplicate ingestion.".getBytes());

        // 1st time ingest
        int firstCount = documentIngestionService.ingestFile(file);
        assertThat(firstCount).isGreaterThan(0);

        // 2nd time ingest should return 0 (skipped)
        int secondCount = documentIngestionService.ingestFile(file);
        assertThat(secondCount).isEqualTo(0);

        // cleanup vector store using contentHash
        ContentHashUtil.HashResult hashResult = ContentHashUtil.calculateHash(file.getResource());
        List<String> ids = jdbcTemplate.queryForList(
                "SELECT id FROM vector_store WHERE metadata->>'contentHash' = ?", String.class, hashResult.hash());
        if (!ids.isEmpty()) {
            vectorStore.delete(ids);
        }
    }
}
