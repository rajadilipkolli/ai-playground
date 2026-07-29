package com.learning.ai.llmragwithspringai.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

class KnowledgeSearchToolTest {

    private VectorStore vectorStore;
    private ToolCallback knowledgeSearchTool;

    @BeforeEach
    void setUp() {
        ToolConfiguration toolConfiguration = new ToolConfiguration();
        vectorStore = mock(VectorStore.class);
        knowledgeSearchTool = toolConfiguration.knowledgeSearchTool(vectorStore);
    }

    @Test
    void shouldFormatRetrievedDocuments() {

        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(List.of(
                        new Document("Document 1 text", Map.of("source", "doc1.pdf", "distance", 0.1)),
                        new Document("Document 2 text", Map.of("source", "doc2.pdf", "distance", 0.2))));

        String result = knowledgeSearchTool.call("{\"query\": \"search query\"}");

        assertThat(result).contains("Source: doc1.pdf (Distance: 0.1)");
        assertThat(result).contains("Document 1 text");
        assertThat(result).contains("---");
        assertThat(result).contains("Source: doc2.pdf (Distance: 0.2)");
        assertThat(result).contains("Document 2 text");
    }

    @Test
    void shouldHandleEmptyResults() {

        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

        String result = knowledgeSearchTool.call("{\"query\": \"search query\"}");

        assertThat(result).contains("No relevant information found.");
    }
}
