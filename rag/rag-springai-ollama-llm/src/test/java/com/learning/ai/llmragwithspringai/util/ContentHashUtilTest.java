package com.learning.ai.llmragwithspringai.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

        String hash1 = ContentHashUtil.calculateHash(resource1).hash();
        String hash2 = ContentHashUtil.calculateHash(resource2).hash();

        assertThat(hash1).isEqualTo(hash2).as("Identical content should produce the same hash");
    }

    @Test
    void testDifferentContentProducesDifferentHash() throws IOException {
        Resource resource1 = createMockResource("Hello, World!");
        Resource resource2 = createMockResource("Goodbye, World!");

        String hash1 = ContentHashUtil.calculateHash(resource1).hash();
        String hash2 = ContentHashUtil.calculateHash(resource2).hash();

        assertThat(hash1).isNotEqualTo(hash2).as("Different content should produce different hashes");
    }

    @Test
    void testHashLengthAndFormat() throws IOException {
        Resource resource = createMockResource("Test content");
        String hash = ContentHashUtil.calculateHash(resource).hash();

        assertThat(hash.length()).isEqualTo(64).as("SHA-256 hex string should be exactly 64 characters long");
        assertThat(hash).matches("^[0-9a-f]{64}$").as("Hash should be a valid hex string");
    }

    @Test
    void testErrorHandling() throws IOException {
        Resource errorResource = mock(Resource.class);
        when(errorResource.getFilename()).thenReturn("error.txt");
        when(errorResource.getInputStream()).thenThrow(new IOException("Simulated read error"));

        assertThatThrownBy(() -> ContentHashUtil.calculateHash(errorResource))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to read resource for hashing: error.txt");
    }
}
