package com.learning.ai.llmragwithspringai.agent;

import static org.assertj.core.api.Assertions.assertThat;

import com.learning.ai.llmragwithspringai.agent.api.AgentQuery;
import com.learning.ai.llmragwithspringai.agent.api.AgentResult;
import com.learning.ai.llmragwithspringai.agent.api.Orchestrator;
import com.learning.ai.llmragwithspringai.config.AbstractIntegrationTest;
import com.learning.ai.llmragwithspringai.service.DataIndexerService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(
        properties = {
            "rag.agent.enabled=true",
            "rag.agent.memory.persistent=false",
            "spring.ai.ollama.chat.model=llama3.2:1b"
        })
public class AgentE2EIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private Orchestrator orchestrator;

    @Autowired
    private DataIndexerService dataIndexerService;

    @Test
    void testAgentPipeline() {
        ByteArrayResource resource =
                new ByteArrayResource("Spring AI supports intelligent agents with memory and tools.".getBytes()) {
                    @Override
                    public String getFilename() {
                        return "test-agent-doc.txt";
                    }
                };
        dataIndexerService.loadData(resource, "test-doc", "tester", "agent");

        AgentResult result = orchestrator.run(new AgentQuery(
                "What does Spring AI support regarding intelligent agents?",
                UUID.randomUUID().toString()));

        assertThat(result).isNotNull();
        assertThat(result.answer()).isNotBlank();
        // Since we explicitly index data, the agent should ideally use a retrieval step and retrieve it
        // Depending on LLM, it might vary, but we can check if it returns something
    }
}
