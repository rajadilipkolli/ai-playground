package com.learning.ai.llmragwithspringai.service;

import static com.learning.ai.llmragwithspringai.util.TestResourceUtil.createMockResource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.learning.ai.llmragwithspringai.model.response.IngestionResult;
import com.learning.ai.llmragwithspringai.model.response.IngestionStatus;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class DataIndexerServiceTest {

    @Mock
    private TokenTextSplitter tokenTextSplitter;

    @Mock
    private VectorStore vectorStore;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private MeterRegistry meterRegistry;

    @Mock
    private Timer timer;

    @Mock
    private Counter counter;

    @InjectMocks
    private DataIndexerService dataIndexerService;

    @BeforeEach
    void setUp() {
        lenient().when(meterRegistry.timer(anyString())).thenReturn(timer);
        lenient().when(meterRegistry.counter(anyString())).thenReturn(counter);
    }

    @Test
    void testSkipDuplicateContentSameFilename() {
        Resource resource = createMockResource("test.txt", "Some content");

        Document existingDoc = new Document("doc-123", "existing-content", Collections.emptyMap());

        // First JdbcTemplate query is for content_hash
        when(jdbcTemplate.queryForList(anyString(), eq(String.class), anyString()))
                .thenReturn(List.of("doc-123"));

        IngestionResult result = dataIndexerService.loadData(resource);

        assertThat(result.status()).isEqualTo(IngestionStatus.SKIPPED_DUPLICATE);
        assertThat(result.filename()).isEqualTo("test.txt");
        assertThat(result.chunksIngested()).isEqualTo(0);
        assertThat(result.chunksDeleted()).isEqualTo(0);

        verify(vectorStore, never()).accept(anyList());
        verify(vectorStore, never()).delete(anyList());
    }

    @Test
    void testReplaceChangedContentSameFilename() {
        Resource resource = createMockResource("test.txt", "New modified content");

        Document oldDoc = new Document("doc-123", "old-content", Collections.emptyMap());

        Document newDoc = new Document("New modified content");
        when(tokenTextSplitter.apply(anyList())).thenReturn(List.of(newDoc));

        // First query (hash) -> empty
        // Second query (filename) -> returns old document
        when(jdbcTemplate.queryForList(anyString(), eq(String.class), anyString()))
                .thenReturn(Collections.emptyList())
                .thenReturn(List.of("doc-123"));

        IngestionResult result = dataIndexerService.loadData(resource);

        assertThat(result.status()).isEqualTo(IngestionStatus.REPLACED);
        assertThat(result.filename()).isEqualTo("test.txt");
        assertThat(result.chunksIngested()).isEqualTo(1);
        assertThat(result.chunksDeleted()).isEqualTo(1);

        verify(vectorStore).delete(List.of("doc-123"));
        verify(vectorStore).accept(anyList());
    }

    @Test
    void testSkipDuplicateContentDifferentFilename() {
        Resource resource = createMockResource("new-file.txt", "Identical content");

        Document existingDoc = new Document("doc-999", "Identical content", Collections.emptyMap());

        // First query (hash) -> returns existing document
        when(jdbcTemplate.queryForList(anyString(), eq(String.class), anyString()))
                .thenReturn(List.of("doc-999"));

        IngestionResult result = dataIndexerService.loadData(resource);

        assertThat(result.status()).isEqualTo(IngestionStatus.SKIPPED_DUPLICATE);
        assertThat(result.filename()).isEqualTo("new-file.txt");
        assertThat(result.chunksIngested()).isEqualTo(0);
        assertThat(result.chunksDeleted()).isEqualTo(0);

        verify(vectorStore, never()).delete(anyList());
        verify(vectorStore, never()).accept(anyList());
    }

    @Test
    void testIngestNewFile() {
        Resource resource = createMockResource("brand-new.txt", "Fresh content");

        Document newDoc = new Document("Fresh content");
        when(tokenTextSplitter.apply(anyList())).thenReturn(List.of(newDoc));

        // First query (hash) -> empty
        // Second query (filename) -> empty
        when(jdbcTemplate.queryForList(anyString(), eq(String.class), anyString()))
                .thenReturn(Collections.emptyList());

        IngestionResult result = dataIndexerService.loadData(resource);

        assertThat(result.status()).isEqualTo(IngestionStatus.INGESTED);
        assertThat(result.filename()).isEqualTo("brand-new.txt");
        assertThat(result.chunksIngested()).isEqualTo(1);
        assertThat(result.chunksDeleted()).isEqualTo(0);

        verify(vectorStore, never()).delete(anyList());
        verify(vectorStore).accept(anyList());
    }
}
