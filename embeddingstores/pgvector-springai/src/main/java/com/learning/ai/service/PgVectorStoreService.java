package com.learning.ai.service;

import com.learning.ai.model.response.AIChatResponse;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

@Service
public class PgVectorStoreService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PgVectorStoreService.class);

    private final VectorStore vectorStore;

    public PgVectorStoreService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public void storeEmbeddings() {
        // Store embeddings
        List<Document> documents = List.of(
                new Document("I like football.", Map.of("userId", 1)),
                new Document("I like cricket.", Map.of("userId", 2)),
                new Document("The weather is good today."));
        vectorStore.add(documents);
    }

    public AIChatResponse queryEmbeddingStore(String question, Integer userId) {
        // Retrieve embeddings
        var queryBuilder = SearchRequest.builder().query(question).topK(1);
        if (userId != null) {
            queryBuilder.filterExpression("userId == " + userId);
        }
        List<Document> similarDocuments = vectorStore.similaritySearch(queryBuilder.build());
        String relevantData =
                similarDocuments.stream().map(Document::getText).collect(Collectors.joining(System.lineSeparator()));

        LOGGER.info("response from vectorStore : {} ", relevantData);
        return new AIChatResponse(relevantData);
    }
}
