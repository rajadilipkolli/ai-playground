package com.learning.ai.llmragwithspringai.agent.impl;

import static org.assertj.core.api.Assertions.assertThat;

import com.learning.ai.llmragwithspringai.agent.api.MemoryEntry;
import com.learning.ai.llmragwithspringai.config.properties.AgentProperties;
import java.util.List;
import org.junit.jupiter.api.Test;

class InMemoryMemoryStoreTest {

    @Test
    void testAddAndGet() {
        AgentProperties props = new AgentProperties();
        props.getMemory().setMaxSize(10);
        props.getMemory().setTtlSeconds(3600);

        InMemoryMemoryStore store = new InMemoryMemoryStore(props);
        store.add("session1", new MemoryEntry("user", "test"));

        List<MemoryEntry> entries = store.get("session1");
        assertThat(entries).hasSize(1);
        assertThat(entries.getFirst().role()).isEqualTo("user");
        assertThat(entries.getFirst().content()).isEqualTo("test");

        store.clear("session1");
        assertThat(store.get("session1")).isEmpty();
    }
}
