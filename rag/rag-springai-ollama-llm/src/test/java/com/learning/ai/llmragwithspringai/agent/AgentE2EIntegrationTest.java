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

@TestPropertySource(properties = {"rag.agent.enabled=true", "rag.agent.memory.persistent=false"})
class AgentE2EIntegrationTest extends AbstractIntegrationTest {

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

        // Verify that the indexed document influenced the execution
        boolean foundInProvenance = result.provenance() != null
                && result.provenance().stream()
                        .anyMatch(p -> p.text() != null && p.text().contains("memory"));

        boolean foundInAnswer = result.answer().toLowerCase().contains("memory")
                || result.answer().toLowerCase().contains("tool");

        assertThat(foundInProvenance || foundInAnswer)
                .as("Expected the indexed document to influence the result (either via provenance or answer)")
                .isTrue();
    }
}
