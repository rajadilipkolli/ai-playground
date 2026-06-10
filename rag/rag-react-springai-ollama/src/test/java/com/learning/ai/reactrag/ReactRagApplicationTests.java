package com.learning.ai.reactrag;

import static org.assertj.core.api.Assertions.assertThat;

import com.learning.ai.reactrag.common.ContainerConfig;
import com.learning.ai.reactrag.model.request.ChatRequest;
import com.learning.ai.reactrag.model.response.ChatResponse;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = ContainerConfig.class)
@AutoConfigureRestTestClient
class ReactRagApplicationTests {

    @LocalServerPort
    private int port;

    @Autowired
    private RestClient.Builder restClientBuilder;

    @Autowired
    private VectorStore vectorStore;

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
        vectorStore.accept(List.of(new Document("The company's secret project is named Project Antigravity.")));

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
}
