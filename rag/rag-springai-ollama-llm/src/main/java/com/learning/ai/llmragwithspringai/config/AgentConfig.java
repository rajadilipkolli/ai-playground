package com.learning.ai.llmragwithspringai.config;

import com.learning.ai.llmragwithspringai.agent.api.MemoryStore;
import com.learning.ai.llmragwithspringai.agent.api.Orchestrator;
import com.learning.ai.llmragwithspringai.agent.api.Planner;
import com.learning.ai.llmragwithspringai.agent.api.ToolRegistry;
import com.learning.ai.llmragwithspringai.agent.impl.*;
import com.learning.ai.llmragwithspringai.config.properties.AgentProperties;
import java.util.List;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.json.JsonMapper;

@Configuration
@ConditionalOnProperty(name = "rag.agent.enabled", havingValue = "true")
public class AgentConfig {

    @Bean
    public Planner planner(
            ChatClient.Builder chatClientBuilder,
            OllamaChatModel ollamaChatModel,
            AgentProperties agentProperties,
            JsonMapper jsonMapper,
            @Value("classpath:/prompts/agent-planner-prompt.txt") Resource systemPromptResource) {
        return new LlmPlanner(chatClientBuilder, ollamaChatModel, agentProperties, jsonMapper, systemPromptResource);
    }

    @Bean
    public ToolRegistry toolRegistry(List<ToolCallback> toolCallbacks, JsonMapper jsonMapper) {
        return new SpringAiToolRegistry(toolCallbacks, jsonMapper);
    }

    @Bean
    @ConditionalOnProperty(name = "rag.agent.memory.persistent", havingValue = "false", matchIfMissing = true)
    public MemoryStore inMemoryMemoryStore(AgentProperties agentProperties) {
        return new InMemoryMemoryStore(agentProperties);
    }

    @Bean
    @ConditionalOnProperty(name = "rag.agent.memory.persistent", havingValue = "true")
    public MemoryStore persistentMemoryStore(JdbcTemplate jdbcTemplate) {
        return new PersistentMemoryStore(jdbcTemplate);
    }

    @Bean
    public Orchestrator orchestrator(
            Planner planner,
            ToolRegistry toolRegistry,
            MemoryStore memoryStore,
            DocumentRetriever documentRetriever,
            AgentProperties agentProperties) {
        return new DefaultOrchestrator(planner, toolRegistry, memoryStore, documentRetriever, agentProperties);
    }
}
