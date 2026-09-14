package com.learning.ai.agent.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;

class ToolConfigurationTest {
    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    private final ToolConfiguration config = new ToolConfiguration();

    @Test
    void calculatorToolAdds() {
        ToolCallback tool = config.calculatorTool(meterRegistry);
        String result = tool.call("{\"a\": 5, \"b\": 3, \"operation\": \"add\"}");
        assertThat(result).isEqualTo("8.0");
        assertThat(meterRegistry
                        .timer("agent.tool.latency", "tool", "calculator")
                        .count())
                .isEqualTo(1);
    }

    @Test
    void calculatorToolDividesByZero() {
        ToolCallback tool = config.calculatorTool(meterRegistry);
        String result = tool.call("{\"a\": 5, \"b\": 0, \"operation\": \"divide\"}");
        assertThat(result).contains("Error: Division by zero");
    }

    @Test
    void calculatorToolRejectsUnsafeInput() {
        ToolCallback tool = config.calculatorTool(meterRegistry);
        String result =
                tool.call("{\"a\": 5, \"b\": 3, \"operation\": \"T(java.lang.Runtime).getRuntime().exec('calc')\"}");
        assertThat(result).contains("Unsafe or invalid operation detected");
        assertThat(meterRegistry
                        .counter("agent.tool.errors", "tool", "calculator")
                        .count())
                .isEqualTo(1.0);
    }

    @Test
    void weatherLookupToolSuccess() {
        ToolCallback tool = config.weatherLookupTool(meterRegistry);
        String result = tool.call("{\"city\": \"London\"}");
        assertThat(result).contains("London");
    }

    @Test
    void currentDateTimeToolSuccess() {
        ToolCallback tool = config.currentDateTimeTool(meterRegistry);
        String result = tool.call("{}");
        assertThat(result).isNotBlank();
    }
}
