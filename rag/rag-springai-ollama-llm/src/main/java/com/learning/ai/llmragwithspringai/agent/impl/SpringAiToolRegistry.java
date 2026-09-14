package com.learning.ai.llmragwithspringai.agent.impl;

import com.learning.ai.llmragwithspringai.agent.api.ToolRegistry;
import com.learning.ai.llmragwithspringai.agent.api.ToolResult;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.ai.tool.ToolCallback;
import tools.jackson.databind.json.JsonMapper;

public class SpringAiToolRegistry implements ToolRegistry {

    private final Map<String, ToolCallback> tools;
    private final JsonMapper jsonMapper;

    public SpringAiToolRegistry(List<ToolCallback> toolCallbacks, JsonMapper jsonMapper) {
        this.tools = toolCallbacks.stream()
                .collect(Collectors.toMap(t -> t.getToolDefinition().name(), Function.identity()));
        this.jsonMapper = jsonMapper;
    }

    @Override
    public Optional<ToolResult> execute(String toolName, Map<String, Object> arguments) {
        ToolCallback callback = tools.get(toolName);
        if (callback == null) {
            return Optional.empty();
        }

        String jsonArgs = arguments == null ? "{}" : jsonMapper.writeValueAsString(arguments);
        String result = callback.call(jsonArgs);
        return Optional.of(new ToolResult(result));
    }

    @Override
    public boolean hasTool(String toolName) {
        return tools.containsKey(toolName);
    }
}
