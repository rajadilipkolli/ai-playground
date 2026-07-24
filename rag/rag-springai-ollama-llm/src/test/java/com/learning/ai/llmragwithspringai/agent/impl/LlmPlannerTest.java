package com.learning.ai.llmragwithspringai.agent.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.learning.ai.llmragwithspringai.agent.api.AgentGoal;
import com.learning.ai.llmragwithspringai.agent.api.PlanStep;
import com.learning.ai.llmragwithspringai.config.properties.AgentProperties;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.core.io.ByteArrayResource;
import tools.jackson.databind.json.JsonMapper;

class LlmPlannerTest {

    @Test
    void testPlanParsingAndCapping() {
        ChatClient chatClient = mock(ChatClient.class);
        ChatClient.CallResponseSpec callResponseSpec = mock(ChatClient.CallResponseSpec.class);
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);

        when(chatClient.prompt(any(Prompt.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn("""
            ```json
            [
              {"type": "tool", "prompt": "use tool", "toolName": "calc", "args": {}},
              {"type": "tool", "prompt": "use tool 2", "toolName": "calc2", "args": {}},
              {"type": "finish", "prompt": "done", "toolName": null, "args": {}}
            ]
            ```
            """);

        AgentProperties props = new AgentProperties();
        props.getPlanner().setMaxSteps(2);

        ChatClient.Builder builder = mock(ChatClient.Builder.class);
        when(builder.defaultOptions(any())).thenReturn(builder);
        when(builder.build()).thenReturn(chatClient);

        OllamaChatModel ollamaChatModel = mock(OllamaChatModel.class);
        when(ollamaChatModel.getOptions())
                .thenReturn(OllamaChatOptions.builder().build());

        LlmPlanner planner = new LlmPlanner(
                builder,
                ollamaChatModel,
                props,
                JsonMapper.builder().build(),
                new ByteArrayResource("system".getBytes()));

        List<PlanStep> steps = planner.plan(new AgentGoal("test"), "context");

        // The input has 3 steps, but maxSteps is 2, so it should be capped.
        assertThat(steps).hasSize(2);
        assertThat(steps.get(0).type()).isEqualTo("tool");
        assertThat(steps.get(1).type()).isEqualTo("tool");
    }
}
