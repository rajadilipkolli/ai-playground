package com.learning.ai.llmragwithspringai.agent.example;

import com.learning.ai.llmragwithspringai.agent.api.AgentQuery;
import com.learning.ai.llmragwithspringai.agent.api.AgentResult;
import com.learning.ai.llmragwithspringai.agent.api.Orchestrator;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = {"rag.agent.enabled", "rag.agent.example.run"},
        havingValue = "true")
public class ExampleRagAgent implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ExampleRagAgent.class);
    private final Orchestrator orchestrator;

    public ExampleRagAgent(Orchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("Running Example Rag Agent...");
        String query = "What is the current time and what is 5 plus 5?";
        AgentResult result =
                orchestrator.run(new AgentQuery(query, UUID.randomUUID().toString()));
        log.info("Agent Answer: {}", result.answer());
        log.info("Provenance items: {}", result.provenance().size());
    }
}
