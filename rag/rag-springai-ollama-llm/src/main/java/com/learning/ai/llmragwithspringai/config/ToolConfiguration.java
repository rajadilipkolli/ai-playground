package com.learning.ai.llmragwithspringai.config;

import com.learning.ai.llmragwithspringai.rag.retrieval.FilterContext;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.objecthunter.exp4j.ExpressionBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class ToolConfiguration {
    private static final Logger log = LoggerFactory.getLogger(ToolConfiguration.class);

    @Bean
    ToolCallback currentDateTimeTool() {
        return FunctionToolCallback.builder("currentDateTimeTool", () -> {
                    log.info("fetching from tool in ollama model");
                    return LocalDateTime.now().toString();
                })
                .description("Get the current date and time or as of today/now.")
                .build();
    }

    public record CalculatorInput(String expression) {}

    @Bean
    ToolCallback calculatorTool() {
        return FunctionToolCallback.builder("calculatorTool", (Function<CalculatorInput, String>) input -> {
                    try {
                        double result = new ExpressionBuilder(input.expression())
                                .build()
                                .evaluate();
                        return String.valueOf(result);
                    } catch (Exception e) {
                        return "Error evaluating expression: " + e.getMessage();
                    }
                })
                .description("Evaluates a mathematical expression. Supports operators: +, -, *, /, ^, and parentheses.")
                .inputType(CalculatorInput.class)
                .build();
    }

    public record SearchInput(String query) {}

    @Bean
    ToolCallback knowledgeSearchTool(VectorStore vectorStore) {
        return FunctionToolCallback.builder("knowledgeSearchTool", (Function<SearchInput, String>) input -> {
                    List<Document> docs = vectorStore.similaritySearch(
                            SearchRequest.builder().query(input.query()).topK(4).build());
                    FilterContext.setRetrievedDocuments(docs);

                    if (docs.isEmpty()) {
                        return "No relevant information found.";
                    }
                    return docs.stream()
                            .map(d -> {
                                String text = d.getText();
                                String source = d.getMetadata()
                                        .getOrDefault("source", "unknown")
                                        .toString();
                                Object distanceObj = d.getMetadata().getOrDefault("distance", 0.0);
                                return String.format(
                                        "Source: %s (Distance: %s)\n%s\n", source, distanceObj.toString(), text);
                            })
                            .collect(Collectors.joining("\n---\n"));
                })
                .description(
                        "Search the knowledge base for domain-specific information, product details, or company data.")
                .inputType(SearchInput.class)
                .build();
    }
}
