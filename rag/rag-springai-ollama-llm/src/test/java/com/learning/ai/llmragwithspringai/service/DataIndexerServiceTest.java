package com.learning.ai.llmragwithspringai.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

import com.learning.ai.llmragwithspringai.model.response.IngestionResult;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.Resource;

@ExtendWith(MockitoExtension.class)
class DataIndexerServiceTest {

    @Mock
    private TokenTextSplitter tokenTextSplitter;

    @Mock
    private VectorStore vectorStore;

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

    private Resource createMockResource(String filename, String content) {
        Resource resource = mock(Resource.class);
        lenient().when(resource.getFilename()).thenReturn(filename);
        try {
            lenient()
                    .when(resource.getInputStream())
                    .thenAnswer(inv -> new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return resource;
    }

    @Test
    void testSkipDuplicateContent_SameFilename() {
        Resource resource = createMockResource("test.txt", "Some content");

        Document existingDoc = new Document("doc-123", "existing-content", Collections.emptyMap());

        // First similaritySearch is for content_hash
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(existingDoc));

        IngestionResult result = dataIndexerService.loadData(resource);

        assertEquals("skipped_duplicate", result.status());
        assertEquals("test.txt", result.filename());
        assertEquals(0, result.chunksIngested());
        assertEquals(0, result.chunksDeleted());

        verify(vectorStore, never()).accept(anyList());
        verify(vectorStore, never()).delete(anyList());
    }

    @Test
    void testReplaceChangedContent_SameFilename() {
        Resource resource = createMockResource("test.txt", "New modified content");

        Document oldDoc = new Document("doc-123", "old-content", Collections.emptyMap());

        Document newDoc = new Document("New modified content");
        when(tokenTextSplitter.apply(anyList())).thenReturn(List.of(newDoc));

        // First similaritySearch (hash) -> empty
        // Second similaritySearch (filename) -> returns old document
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(Collections.emptyList())
                .thenReturn(List.of(oldDoc));

        IngestionResult result = dataIndexerService.loadData(resource);

        assertEquals("replaced", result.status());
        assertEquals("test.txt", result.filename());
        assertTrue(result.chunksDeleted() > 0);
        assertEquals(1, result.chunksIngested());

        verify(vectorStore).delete(List.of("doc-123"));
        verify(vectorStore).accept(anyList());
    }

    @Test
    void testSkipDuplicateContent_DifferentFilename() {
        Resource resource = createMockResource("new-file.txt", "Identical content");

        Document existingDoc = new Document("doc-999", "Identical content", Collections.emptyMap());

        // First similaritySearch (hash) -> returns existing document
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(existingDoc));

        IngestionResult result = dataIndexerService.loadData(resource);

        assertEquals("skipped_duplicate", result.status());
        assertEquals(0, result.chunksIngested());
        assertEquals(0, result.chunksDeleted());

        verify(vectorStore, never()).delete(anyList());
        verify(vectorStore, never()).accept(anyList());
    }

    @Test
    void testIngestNewFile() {
        Resource resource = createMockResource("brand-new.txt", "Fresh content");

        Document newDoc = new Document("Fresh content");
        when(tokenTextSplitter.apply(anyList())).thenReturn(List.of(newDoc));

        // First similaritySearch (hash) -> empty
        // Second similaritySearch (filename) -> empty
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(Collections.emptyList());

        IngestionResult result = dataIndexerService.loadData(resource);

        assertEquals("ingested", result.status());
        assertTrue(result.chunksIngested() > 0);
        assertEquals(0, result.chunksDeleted());

        verify(vectorStore, never()).delete(anyList());
        verify(vectorStore).accept(anyList());
    }
}
