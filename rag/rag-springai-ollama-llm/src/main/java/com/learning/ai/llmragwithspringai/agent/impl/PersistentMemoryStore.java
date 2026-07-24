package com.learning.ai.llmragwithspringai.agent.impl;

import com.learning.ai.llmragwithspringai.agent.api.MemoryEntry;
import com.learning.ai.llmragwithspringai.agent.api.MemoryStore;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;

public class PersistentMemoryStore implements MemoryStore {

    private final JdbcTemplate jdbcTemplate;

    public PersistentMemoryStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void add(String sessionId, MemoryEntry entry) {
        String sql = "INSERT INTO agent_memory (session_id, role, content) VALUES (?, ?, ?)";
        jdbcTemplate.update(sql, sessionId, entry.role(), entry.content());
    }

    @Override
    public List<MemoryEntry> get(String sessionId) {
        String sql = "SELECT role, content FROM agent_memory WHERE session_id = ? ORDER BY created_at ASC, id ASC";
        return jdbcTemplate.query(
                sql, (rs, rowNum) -> new MemoryEntry(rs.getString("role"), rs.getString("content")), sessionId);
    }

    @Override
    public void clear(String sessionId) {
        String sql = "DELETE FROM agent_memory WHERE session_id = ?";
        jdbcTemplate.update(sql, sessionId);
    }
}
