package com.learning.ai.llmragwithspringai.rag.query;

import com.learning.ai.llmragwithspringai.config.properties.RagQueryProperties;
import java.util.ArrayList;
import java.util.List;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.preretrieval.query.expansion.QueryExpander;

public class MultiQueryExpander implements QueryExpander {

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

        String response = chatClient.prompt().user(prompt).call().content();

        List<Query> queries = new ArrayList<>();
        // Always include the original query
        queries.add(query);

        if (response != null && !response.isBlank()) {
            String[] generatedQueries = response.split("\\r?\\n");
            for (String generated : generatedQueries) {
                String cleanQuery = generated.trim();
                // Basic cleanup of list artifacts like "1. ", "- ", etc.
                cleanQuery = cleanQuery.replaceAll("^(?:\\d+\\.|[-*])\\s+", "");
                if (!cleanQuery.isBlank() && !cleanQuery.equals(query.text())) {
                    queries.add(new Query(cleanQuery));
                }
            }
        }

        return queries;
    }
}
