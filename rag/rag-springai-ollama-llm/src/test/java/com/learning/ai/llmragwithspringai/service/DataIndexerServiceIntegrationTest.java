package com.learning.ai.llmragwithspringai.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import com.learning.ai.llmragwithspringai.config.AbstractIntegrationTest;
import com.learning.ai.llmragwithspringai.model.response.IngestionResult;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.core.io.Resource;

class DataIndexerServiceIntegrationTest extends AbstractIntegrationTest {

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("TRUNCATE TABLE vector_store");
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

    private List<Document> getAllDocuments() {
        return vectorStore.similaritySearch(
                SearchRequest.builder().query("").topK(10000).build());
    }

    @Test
    void testSkipDuplicateContent_SameFilename_Integration() {
        Resource resource = createMockResource("test.txt", "Integration test content");

        IngestionResult result1 = dataIndexerService.loadData(resource);
        assertEquals("ingested", result1.status());

        List<Document> initialDocs = getAllDocuments();
        int initialCount = initialDocs.size();
        assertTrue(initialCount > 0);

        IngestionResult result2 = dataIndexerService.loadData(resource);
        assertEquals("skipped_duplicate", result2.status());
        assertEquals(0, result2.chunksIngested());
        assertEquals(0, result2.chunksDeleted());

        List<Document> finalDocs = getAllDocuments();
        assertEquals(initialCount, finalDocs.size());
    }

    @Test
    void testReplaceChangedContent_SameFilename_Integration() {
        Resource resource1 = createMockResource("test.txt", "version 1");
        IngestionResult result1 = dataIndexerService.loadData(resource1);
        assertEquals("ingested", result1.status());

        List<Document> docsV1 = getAllDocuments();
        int chunksV1 = docsV1.size();
        assertTrue(chunksV1 > 0);

        Resource resource2 = createMockResource("test.txt", "version 2 with more text");
        IngestionResult result2 = dataIndexerService.loadData(resource2);
        assertEquals("replaced", result2.status());
        assertEquals(chunksV1, result2.chunksDeleted());
        assertTrue(result2.chunksIngested() > 0);

        List<Document> docsV2 = getAllDocuments();
        assertEquals(result2.chunksIngested(), docsV2.size());
        for (Document doc : docsV2) {
            assertTrue(doc.getText().contains("version 2"));
        }
    }

    @Test
    void testSkipDuplicateContent_DifferentFilename_Integration() {
        Resource resourceA = createMockResource("fileA.txt", "duplicate content");
        IngestionResult resultA = dataIndexerService.loadData(resourceA);
        assertEquals("ingested", resultA.status());

        List<Document> docsA = getAllDocuments();
        int initialCount = docsA.size();
        assertTrue(initialCount > 0);

        Resource resourceB = createMockResource("fileB.txt", "duplicate content");
        IngestionResult resultB = dataIndexerService.loadData(resourceB);
        assertEquals("skipped_duplicate", resultB.status());

        List<Document> finalDocs = getAllDocuments();
        assertEquals(initialCount, finalDocs.size());
        for (Document doc : finalDocs) {
            assertEquals("fileA.txt", doc.getMetadata().get("source_filename"));
        }
    }

    @Test
    void testIngestNewFile_Integration() {
        List<Document> initialDocs = getAllDocuments();
        assertEquals(0, initialDocs.size());

        Resource resource =
                createMockResource("brand-new-integration.txt", "Fresh unique content for integration test");
        IngestionResult result = dataIndexerService.loadData(resource);

        assertEquals("ingested", result.status());
        assertTrue(result.chunksIngested() > 0);
        assertEquals(0, result.chunksDeleted());

        List<Document> finalDocs = getAllDocuments();
        assertEquals(result.chunksIngested(), finalDocs.size());

        for (Document doc : finalDocs) {
            assertEquals("brand-new-integration.txt", doc.getMetadata().get("source_filename"));
            assertNotNull(doc.getMetadata().get("content_hash"));
            assertNotNull(doc.getMetadata().get("ingested_at"));
            assertEquals("true", doc.getMetadata().get("EXTERNAL_KNOWLEDGE"));
        }
    }
}
