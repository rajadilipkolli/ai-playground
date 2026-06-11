package com.learning.ai.reactrag.config;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.objecthunter.exp4j.ExpressionBuilder;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class ToolConfiguration {

    public record CalculatorInput(String expression) {}

    public record SearchInput(String query) {}

    @Bean
    public ToolCallback currentDateTimeTool() {
        return FunctionToolCallback.builder(
                        "currentDateTimeTool", () -> LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .description(
                        "Returns the current date and time. Use this when the user asks about the current date, time, or relative dates like 'today' or 'now'.")
                .build();
    }

    @Bean
    public ToolCallback calculatorTool() {
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
                .description(
                        "Evaluates a mathematical expression string (e.g., '2 + 2 * 3') and returns the numeric result. Use this for all math calculations.")
                .inputType(CalculatorInput.class)
                .build();
    }

    @Bean
    public ToolCallback knowledgeSearchTool(VectorStore vectorStore) {
        return FunctionToolCallback.builder("knowledgeSearchTool", (Function<SearchInput, String>) input -> {
                    List<Document> results = vectorStore.similaritySearch(
                            SearchRequest.builder().query(input.query()).topK(3).build());
                    if (results.isEmpty()) {
                        return "No relevant information found.";
                    }
                    return results.stream().map(Document::getText).collect(Collectors.joining("\n---\n"));
                })
                .description(
                        "Searches the knowledge base for relevant document snippets based on a query string. Use this when the user asks questions about specific domain knowledge, products, or company information.")
                .inputType(SearchInput.class)
                .build();
    }
}
