package com.learning.ai.llmragwithspringai.config;

import com.learning.ai.llmragwithspringai.service.DataIndexerService;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ByteArrayResource;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "rag.agent.enabled", havingValue = "true")
class AgentToolConfiguration {
    private static final Logger log = LoggerFactory.getLogger(AgentToolConfiguration.class);

    public record KnowledgeInserterInput(String documentText) {}

    @Bean
    @ConditionalOnProperty(name = "rag.agent.tools.knowledge-inserter.enabled", havingValue = "true")
    ToolCallback knowledgeInserterTool(DataIndexerService dataIndexerService) {
        return FunctionToolCallback.builder("knowledgeInserterTool", (Function<KnowledgeInserterInput, String>)
                        input -> {
                            log.info("Inserting document into knowledge base.");
                            try {
                                ByteArrayResource resource = new ByteArrayResource(
                                        input.documentText().getBytes()) {
                                    @Override
                                    public String getFilename() {
                                        return "inserted_knowledge_" + System.currentTimeMillis() + ".txt";
                                    }
                                };
                                dataIndexerService.loadData(resource, "agent-inserted", "agent", "knowledge");
                                return "Document successfully inserted into the knowledge base.";
                            } catch (Exception e) {
                                log.error("Failed to insert document", e);
                                return "Failed to insert document due to an internal error.";
                            }
                        })
                .description("Insert a new document or information into the knowledge base.")
                .inputType(KnowledgeInserterInput.class)
                .build();
    }
}
