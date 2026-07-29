package com.learning.ai.llmragwithspringai.agent.api;

public record AgentQuery(String text, String sessionId) {
    public AgentQuery {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("AgentQuery text cannot be null or blank");
        }
    }
}
