package com.learning.ai.llmragwithspringai.service;

import static com.learning.ai.llmragwithspringai.util.TestResourceUtil.createMockResource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.learning.ai.llmragwithspringai.config.AbstractIntegrationTest;
import com.learning.ai.llmragwithspringai.model.response.IngestionResult;
import com.learning.ai.llmragwithspringai.model.response.IngestionStatus;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.core.io.Resource;
import tools.jackson.core.type.TypeReference;

class DataIndexerServiceIntegrationTest extends AbstractIntegrationTest {

    @BeforeEach
    void cleanUpTestData() {
        // Delete only the documents created by this integration test to avoid breaking other tests
        jdbcTemplate.execute(
                "DELETE FROM vector_store WHERE metadata->>'source_filename' IN ('test.txt', 'test2.txt','fileA.txt', 'fileB.txt', 'brand-new-integration.txt')");
    }

    private List<Document> getDocumentsByFilename(String filename) {
        return jdbcTemplate.query(
                "SELECT content, metadata FROM vector_store WHERE metadata->>'source_filename' = ?",
                (rs, rowNum) -> {
                    String content = rs.getString("content");
                    String metadataJson = rs.getString("metadata");
                    Map<String, Object> metadata = Collections.emptyMap();
                    try {
                        if (metadataJson != null) {
                            metadata = jsonMapper.readValue(metadataJson, new TypeReference<Map<String, Object>>() {});
                        }
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to parse metadata", e);
                    }
                    return new Document(content, metadata);
                },
                filename);
    }

    @Test
    void testSkipDuplicateContentSameFilenameIntegration() {
        Resource resource = createMockResource("test.txt", "Integration test content");

        IngestionResult result1 = dataIndexerService.loadData(resource);
        assertEquals(IngestionStatus.INGESTED, result1.status());

        List<Document> initialDocs = getDocumentsByFilename("test.txt");
        int initialCount = initialDocs.size();
        assertTrue(initialCount > 0);

        IngestionResult result2 = dataIndexerService.loadData(resource);
        assertEquals(IngestionStatus.SKIPPED_DUPLICATE, result2.status());
        assertEquals(0, result2.chunksIngested());
        assertEquals(0, result2.chunksDeleted());

        List<Document> finalDocs = getDocumentsByFilename("test.txt");
        assertEquals(initialCount, finalDocs.size());
    }

    @Test
    void testReplaceChangedContentSameFilenameIntegration() {
        Resource resource1 = createMockResource("test2.txt", "version 1");
        IngestionResult result1 = dataIndexerService.loadData(resource1);
        assertEquals(IngestionStatus.INGESTED, result1.status());

        List<Document> docsV1 = getDocumentsByFilename("test2.txt");
        int chunksV1 = docsV1.size();
        assertTrue(chunksV1 > 0);

        Resource resource2 = createMockResource("test2.txt", "version 2 with more text");
        IngestionResult result2 = dataIndexerService.loadData(resource2);
        assertEquals(IngestionStatus.REPLACED, result2.status());
        assertEquals(chunksV1, result2.chunksDeleted());
        assertTrue(result2.chunksIngested() > 0);

        List<Document> docsV2 = getDocumentsByFilename("test2.txt");
        assertEquals(result2.chunksIngested(), docsV2.size());
        for (Document doc : docsV2) {
            assertTrue(doc.getText().contains("version 2"));
        }
    }

    @Test
    void testSkipDuplicateContentDifferentFilenameIntegration() {
        Resource resourceA = createMockResource("fileA.txt", "duplicate content");
        IngestionResult resultA = dataIndexerService.loadData(resourceA);
        assertEquals(IngestionStatus.INGESTED, resultA.status());

        List<Document> docsA = getDocumentsByFilename("fileA.txt");
        int initialCount = docsA.size();
        assertTrue(initialCount > 0);

        Resource resourceB = createMockResource("fileB.txt", "duplicate content");
        IngestionResult resultB = dataIndexerService.loadData(resourceB);
        assertEquals(IngestionStatus.SKIPPED_DUPLICATE, resultB.status());

        List<Document> finalDocsFileA = getDocumentsByFilename("fileA.txt");
        assertEquals(initialCount, finalDocsFileA.size());
        List<Document> finalDocsFileB = getDocumentsByFilename("fileB.txt");
        assertEquals(0, finalDocsFileB.size());
    }

    @Test
    void testIngestNewFileIntegration() {
        List<Document> initialDocs = getDocumentsByFilename("brand-new-integration.txt");
        assertEquals(0, initialDocs.size());

        Resource resource =
                createMockResource("brand-new-integration.txt", "Fresh unique content for integration test");
        IngestionResult result = dataIndexerService.loadData(resource);

        assertEquals(IngestionStatus.INGESTED, result.status());
        assertTrue(result.chunksIngested() > 0);
        assertEquals(0, result.chunksDeleted());

        List<Document> finalDocs = getDocumentsByFilename("brand-new-integration.txt");
        assertEquals(result.chunksIngested(), finalDocs.size());

        for (Document doc : finalDocs) {
            assertEquals("brand-new-integration.txt", doc.getMetadata().get("source_filename"));
            assertNotNull(doc.getMetadata().get("content_hash"));
            assertNotNull(doc.getMetadata().get("ingested_at"));
            assertEquals("true", doc.getMetadata().get("EXTERNAL_KNOWLEDGE"));
        }
    }
}
