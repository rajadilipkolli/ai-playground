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

    public record WebSearchInput(String query) {}

    @Bean
    @ConditionalOnProperty(name = "rag.agent.tools.web-search.enabled", havingValue = "true")
    ToolCallback webSearchTool() {
        return FunctionToolCallback.builder("webSearchTool", (Function<WebSearchInput, String>) input -> {
                    log.info("Simulating web search for query: {}", input.query());
                    return "Simulated web search result for: " + input.query();
                })
                .description("Search the web for real-time information.")
                .inputType(WebSearchInput.class)
                .build();
    }

    public record CodeLookupInput(String className) {}

    @Bean
    ToolCallback codeLookupTool() {
        return FunctionToolCallback.builder("codeLookupTool", (Function<CodeLookupInput, String>) input -> {
                    log.info("Simulating code lookup for class: {}", input.className());
                    return "Simulated source code for: " + input.className();
                })
                .description("Look up source code details for a given class name.")
                .inputType(CodeLookupInput.class)
                .build();
    }

    public record KnowledgeInserterInput(String documentText) {}

    @Bean
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
                                return "Failed to insert document: " + e.getMessage();
                            }
                        })
                .description("Insert a new document or information into the knowledge base.")
                .inputType(KnowledgeInserterInput.class)
                .build();
    }
}
