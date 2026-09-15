package com.learning.ai.llmragwithspringai.agent.impl;

import static org.assertj.core.api.Assertions.assertThat;

import com.learning.ai.llmragwithspringai.agent.api.MemoryEntry;
import com.learning.ai.llmragwithspringai.agent.api.MemoryStore;
import com.learning.ai.llmragwithspringai.config.AbstractIntegrationTest;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = {"rag.agent.enabled=true", "rag.agent.memory.persistent=true"})
public class PersistentMemoryStoreIT extends AbstractIntegrationTest {

    @Autowired
    private MemoryStore memoryStore;

    private String currentSessionId;

    @AfterEach
    void tearDown() {
        if (currentSessionId != null) {
            memoryStore.clear(currentSessionId);
        }
    }

    @Test
    void testWriteReadAndClearMemory() {
        currentSessionId = UUID.randomUUID().toString();
        memoryStore.add(currentSessionId, new MemoryEntry("user", "Hello!"));
        memoryStore.add(currentSessionId, new MemoryEntry("assistant", "Hi there."));

        List<MemoryEntry> entries = memoryStore.get(currentSessionId);
        assertThat(entries).hasSize(2);
        assertThat(entries.get(0).role()).isEqualTo("user");
        assertThat(entries.get(0).content()).isEqualTo("Hello!");
        assertThat(entries.get(1).role()).isEqualTo("assistant");
        assertThat(entries.get(1).content()).isEqualTo("Hi there.");

        memoryStore.clear(currentSessionId);
        assertThat(memoryStore.get(currentSessionId)).isEmpty();
    }

    @Test
    void testEmptyMemory() {
        currentSessionId = UUID.randomUUID().toString();
        assertThat(memoryStore.get(currentSessionId)).isEmpty();
    }
}
