package com.learning.ai.llmragwithspringai.agent.api;

import java.util.List;

public interface MemoryStore {
    void add(String sessionId, MemoryEntry entry);

    List<MemoryEntry> get(String sessionId);

    void clear(String sessionId);
}
