package com.learning.ai.llmragwithspringai.rag.retrieval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class KeywordDocumentRetrieverTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private KeywordDocumentRetriever retriever;

    @BeforeEach
    void setUp() {
        JsonMapper jsonMapper = JsonMapper.builder().build();
        retriever = new KeywordDocumentRetriever(jdbcTemplate, 5, jsonMapper);
    }

    @Test
    void testRetrieve_WithValidQuery_ReturnsDocuments() {
        Query query = new Query("test query");
        Document mockDoc = Document.builder().id("1").text("test content").build();

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq("test query"), eq("test query"), eq(5)))
                .thenReturn(List.of(mockDoc));

        List<Document> results = retriever.retrieve(query);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getText()).isEqualTo("test content");
    }

    @Test
    void testRetrieve_WithFilter_AppliesFilterToSql() {
        Query query = new Query("test filter");
        Document mockDoc = Document.builder().id("1").text("test content").build();

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq("test filter"), eq("test filter"), eq(5)))
                .thenAnswer(invocation -> {
                    String sql = invocation.getArgument(0);
                    assertThat(sql).contains("metadata::jsonb @@");
                    return List.of(mockDoc);
                });

        ScopedValue.where(FilterContext.FILTER_EXPRESSION, "category == 'tech'").run(() -> {
            List<Document> results = retriever.retrieve(query);
            assertThat(results).hasSize(1);
        });
    }
}
