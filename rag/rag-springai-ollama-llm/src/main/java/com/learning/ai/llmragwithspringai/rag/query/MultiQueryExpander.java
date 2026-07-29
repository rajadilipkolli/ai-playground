package com.learning.ai.llmragwithspringai.rag.query;

import com.learning.ai.llmragwithspringai.config.properties.RagQueryProperties;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.preretrieval.query.expansion.QueryExpander;

public class MultiQueryExpander implements QueryExpander {

    private static final Logger LOGGER = LoggerFactory.getLogger(MultiQueryExpander.class);

    private final ChatClient chatClient;
    private final RagQueryProperties queryProperties;

    public MultiQueryExpander(ChatClient.Builder chatClientBuilder, RagQueryProperties queryProperties) {
        this.chatClient = chatClientBuilder.build();
        this.queryProperties = queryProperties;
    }

    @Override
    public List<Query> expand(Query query) {
        int variationsCount = queryProperties.getMultiquery().getVariations();

        String prompt = String.format("""
                You are an AI language model assistant. Your task is to generate %d
                different versions of the given user question to retrieve relevant documents from a vector database.
                By generating multiple perspectives on the user question, your goal is to help the user overcome
                some of the limitations of distance-based similarity search.

                Provide these alternative questions separated by newlines. Do not include numbered lists or extra text.

                Original question: %s
                """, variationsCount, query.text());

        List<Query> queries = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        // Always include the original query
        queries.add(query);
        seen.add(query.text().toLowerCase());

        String response;
        try {
            response = chatClient.prompt().user(prompt).call().content();
            if (response != null && !response.isBlank()) {
                String[] generatedQueries = response.split("\\r?\\n");
                for (String generated : generatedQueries) {
                    String cleanQuery = generated.trim();
                    // Basic cleanup of list artifacts like "1. ", "- ", etc.
                    cleanQuery = cleanQuery.replaceAll("^(?:\\d+\\.|[-*])\\s+", "");
                    if (!cleanQuery.isBlank() && seen.add(cleanQuery.toLowerCase())) {
                        queries.add(new Query(cleanQuery));
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to expand query, falling back to original query: {}", e.getMessage());
            queries.clear();
            queries.add(query);
        }

        return queries;
    }
}
