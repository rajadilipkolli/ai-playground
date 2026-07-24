package com.learning.ai.llmragwithspringai.agent.impl;

import static org.assertj.core.api.Assertions.assertThat;

import com.learning.ai.llmragwithspringai.agent.api.ToolResult;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import tools.jackson.databind.json.JsonMapper;

class SpringAiToolRegistryTest {

    public record TestInput(String data) {}

    @Test
    void testExecuteTool() {
        ToolCallback callback = FunctionToolCallback.builder(
                        "testTool", (Function<TestInput, String>) input -> "result")
                .inputType(TestInput.class)
                .build();

        SpringAiToolRegistry registry =
                new SpringAiToolRegistry(List.of(callback), JsonMapper.builder().build());

        assertThat(registry.hasTool("testTool")).isTrue();
        assertThat(registry.hasTool("missing")).isFalse();

        Optional<ToolResult> result = registry.execute("testTool", Map.of("data", "value"));
        assertThat(result).isPresent();
        assertThat(result.get().result()).contains("result");
    }
}
