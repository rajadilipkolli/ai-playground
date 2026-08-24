package com.learning.ai.llmragwithspringai.agent.impl;

import com.learning.ai.llmragwithspringai.agent.api.AgentGoal;
import com.learning.ai.llmragwithspringai.agent.api.PlanStep;
import com.learning.ai.llmragwithspringai.agent.api.Planner;
import com.learning.ai.llmragwithspringai.config.properties.AgentProperties;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.core.io.Resource;
import tools.jackson.core.JsonParser;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

public class LlmPlanner implements Planner {

    private final ChatClient chatClient;
    private final AgentProperties.Planner plannerProps;
    private final JsonMapper jsonMapper;
    private final Resource systemPromptResource;

    public LlmPlanner(
            ChatClient.Builder chatClientBuilder,
            OllamaChatModel ollamaChatModel,
            AgentProperties properties,
            JsonMapper jsonMapper,
            Resource systemPromptResource) {
        this.plannerProps = properties.getPlanner();
        this.jsonMapper = jsonMapper;
        this.systemPromptResource = systemPromptResource;

        ChatClient.Builder builder = chatClientBuilder;
        String model = plannerProps.getModel();
        Double temp = plannerProps.getTemperature();

        if ((model != null && !model.isBlank()) || temp != null) {
            OllamaChatOptions defaultOptions = ollamaChatModel.getOptions();
            var optionsBuilder = defaultOptions.mutate();
            if (model != null && !model.isBlank()) {
                optionsBuilder.model(model);
            }
            if (temp != null) {
                optionsBuilder.temperature(temp);
            }
            builder = builder.defaultOptions(optionsBuilder);
        }

        this.chatClient = builder.build();
    }

    @Override
    public List<PlanStep> plan(AgentGoal goal, String context) {
        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(systemPromptResource);
        SystemMessage systemMessage = (SystemMessage) systemPromptTemplate.createMessage();

        String userText = "Goal: " + goal.text() + "\nAccumulated Context: " + context;
        UserMessage userMessage = new UserMessage(userText);

        Prompt prompt = new Prompt(List.of(systemMessage, userMessage));

        String response = chatClient.prompt(prompt).call().content();

        // Ensure response is just the JSON array.
        String json = response;
        if (json.contains("```json")) {
            json = json.substring(json.indexOf("```json") + 7);
            if (json.contains("```")) {
                json = json.substring(0, json.lastIndexOf("```"));
            }
        } else if (json.contains("```")) {
            json = json.substring(json.indexOf("```") + 3);
            if (json.contains("```")) {
                json = json.substring(0, json.lastIndexOf("```"));
            }
        }
        json = json.trim();

        List<PlanStep> steps = new ArrayList<>();
        try (JsonParser parser = jsonMapper.createParser(json)) {
            Iterator<JsonNode> it = jsonMapper.readValues(parser, JsonNode.class);
            while (it.hasNext()) {
                JsonNode node = it.next();
                if (node.isArray()) {
                    steps.addAll(jsonMapper.treeToValue(node, new TypeReference<List<PlanStep>>() {}));
                } else if (node.isObject()) {
                    if (node.has("steps") && node.get("steps").isArray()) {
                        steps.addAll(jsonMapper.treeToValue(node.get("steps"), new TypeReference<List<PlanStep>>() {}));
                    } else {
                        steps.add(jsonMapper.treeToValue(node, PlanStep.class));
                    }
                }
            }
        }
        if (steps.size() > plannerProps.getMaxSteps()) {
            return steps.subList(0, plannerProps.getMaxSteps());
        }
        return steps;
    }
}
