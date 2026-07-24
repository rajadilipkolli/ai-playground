package com.learning.ai.llmragwithspringai.agent.api;

import java.util.Map;
import java.util.Optional;

public interface ToolRegistry {
    Optional<ToolResult> execute(String toolName, Map<String, Object> arguments);

    boolean hasTool(String toolName);
}
