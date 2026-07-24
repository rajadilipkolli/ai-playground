package com.learning.ai.llmragwithspringai.agent.impl;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.learning.ai.llmragwithspringai.agent.api.MemoryEntry;
import com.learning.ai.llmragwithspringai.agent.api.MemoryStore;
import com.learning.ai.llmragwithspringai.config.properties.AgentProperties;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class InMemoryMemoryStore implements MemoryStore {

    private final Cache<String, List<MemoryEntry>> cache;

    public InMemoryMemoryStore(AgentProperties properties) {
        this.cache = Caffeine.newBuilder()
                .expireAfterAccess(properties.getMemory().getTtlSeconds(), TimeUnit.SECONDS)
                .maximumSize(properties.getMemory().getMaxSize())
                .build();
    }

    @Override
    public void add(String sessionId, MemoryEntry entry) {
        cache.asMap().compute(sessionId, (k, v) -> {
            if (v == null) {
                v = new ArrayList<>();
            }
            v.add(entry);
            return v;
        });
    }

    @Override
    public List<MemoryEntry> get(String sessionId) {
        List<MemoryEntry> entries = cache.getIfPresent(sessionId);
        return entries != null ? List.copyOf(entries) : Collections.emptyList();
    }

    @Override
    public void clear(String sessionId) {
        cache.invalidate(sessionId);
    }
}
