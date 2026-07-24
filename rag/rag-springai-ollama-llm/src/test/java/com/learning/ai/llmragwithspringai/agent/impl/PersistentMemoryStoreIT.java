package com.learning.ai.llmragwithspringai.agent.impl;

import static org.assertj.core.api.Assertions.assertThat;

import com.learning.ai.llmragwithspringai.agent.api.MemoryEntry;
import com.learning.ai.llmragwithspringai.agent.api.MemoryStore;
import com.learning.ai.llmragwithspringai.config.AbstractIntegrationTest;
import java.util.List;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = {"rag.agent.enabled=true", "rag.agent.memory.persistent=true"})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PersistentMemoryStoreIT extends AbstractIntegrationTest {

    @Autowired
    private MemoryStore memoryStore;

    private static final String SESSION_ID = "test-session-123";

    @Test
    @Order(1)
    void writeToMemory() {
        memoryStore.add(SESSION_ID, new MemoryEntry("user", "Hello!"));
        memoryStore.add(SESSION_ID, new MemoryEntry("assistant", "Hi there."));
    }

    @Test
    @Order(2)
    void readFromMemoryAndVerify() {
        List<MemoryEntry> entries = memoryStore.get(SESSION_ID);
        assertThat(entries).hasSize(2);
        assertThat(entries.get(0).role()).isEqualTo("user");
        assertThat(entries.get(0).content()).isEqualTo("Hello!");
        assertThat(entries.get(1).role()).isEqualTo("assistant");
        assertThat(entries.get(1).content()).isEqualTo("Hi there.");

        memoryStore.clear(SESSION_ID);
        assertThat(memoryStore.get(SESSION_ID)).isEmpty();
    }
}
