package com.learning.ai.llmragwithspringai.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;

class CalculatorToolTest {

    private ToolConfiguration toolConfiguration;
    private ToolCallback calculatorTool;

    @BeforeEach
    void setUp() {
        toolConfiguration = new ToolConfiguration();
        calculatorTool = toolConfiguration.calculatorTool();
    }

    @Test
    void shouldEvaluateValidExpression() {

        String result = calculatorTool.call("{\"expression\": \"2 + 2 * 3\"}");
        assertThat(result).contains("8.0");

        String resultWithParentheses = calculatorTool.call("{\"expression\": \"(2 + 2) * 3\"}");
        assertThat(resultWithParentheses).contains("12.0");

        String resultWithExponent = calculatorTool.call("{\"expression\": \"2 ^ 3\"}");
        assertThat(resultWithExponent).contains("8.0");
    }

    @Test
    void shouldReturnErrorMessageForInvalidExpression() {

        String result = calculatorTool.call("{\"expression\": \"2 + \"}");
        assertThat(result).contains("Error evaluating expression:");
    }

    @Test
    void shouldRejectSpelInjection() {

        // exp4j does not execute SpEL or Java code, it only parses math expressions
        String result = calculatorTool.call("{\"expression\": \"T(java.lang.Runtime).getRuntime().exec('calc')\"}");
        assertThat(result).contains("Error evaluating expression:");
    }
}
