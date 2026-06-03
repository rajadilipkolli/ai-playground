package com.learning.ai.llmragwithspringai.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;

class ContentHashUtilTest {

    private Resource createMockResource(String content) throws IOException {
        Resource resource = mock(Resource.class);
        when(resource.getInputStream())
                .thenAnswer(inv -> new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
        return resource;
    }

    @Test
    void testSameContentProducesSameHash() throws IOException {
        Resource resource1 = createMockResource("Hello, World!");
        Resource resource2 = createMockResource("Hello, World!");

        String hash1 = ContentHashUtil.calculateHash(resource1);
        String hash2 = ContentHashUtil.calculateHash(resource2);

        assertEquals(hash1, hash2, "Identical content should produce the same hash");
    }

    @Test
    void testDifferentContentProducesDifferentHash() throws IOException {
        Resource resource1 = createMockResource("Hello, World!");
        Resource resource2 = createMockResource("Goodbye, World!");

        String hash1 = ContentHashUtil.calculateHash(resource1);
        String hash2 = ContentHashUtil.calculateHash(resource2);

        assertNotEquals(hash1, hash2, "Different content should produce different hashes");
    }

    @Test
    void testHashLengthAndFormat() throws IOException {
        Resource resource = createMockResource("Test content");
        String hash = ContentHashUtil.calculateHash(resource);

        assertEquals(64, hash.length(), "SHA-256 hex string should be exactly 64 characters long");
        assertTrue(hash.matches("^[0-9a-f]{64}$"), "Hash should be a valid hex string");
    }

    @Test
    void testErrorHandling() throws IOException {
        Resource errorResource = mock(Resource.class);
        when(errorResource.getFilename()).thenReturn("error.txt");
        when(errorResource.getInputStream()).thenThrow(new IOException("Simulated read error"));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            ContentHashUtil.calculateHash(errorResource);
        });

        assertTrue(
                exception.getMessage().contains("Failed to read resource for hashing: error.txt"),
                "Exception message should indicate the failure");
        assertTrue(exception.getCause() instanceof IOException, "Cause should be the underlying IOException");
    }
}
