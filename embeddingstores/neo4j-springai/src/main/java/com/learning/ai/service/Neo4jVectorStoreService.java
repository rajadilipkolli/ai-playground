package com.learning.ai.service;

import com.learning.ai.model.response.AIChatResponse;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

@Service
public class Neo4jVectorStoreService {

    private static final Logger LOGGER = LoggerFactory.getLogger(Neo4jVectorStoreService.class);

    private final VectorStore vectorStore;

    public Neo4jVectorStoreService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public void storeEmbeddings() {
        // Store embeddings
        List<Document> documents =
                List.of(new Document("I like football."), new Document("The weather is good today."));
        vectorStore.add(documents);
        LOGGER.info("Added initial documents");
    }

    public AIChatResponse queryEmbeddingStore(String question) {
        try {
            // Retrieve embeddings
            SearchRequest query =
                    SearchRequest.builder().query(question).topK(1).build();
            List<Document> similarDocuments = vectorStore.similaritySearch(query);

            if (similarDocuments.isEmpty()) {
                // Handle case where no similar documents are found
                LOGGER.info("No similar documents found for the question: {}", question);
                return new AIChatResponse("No similar documents found.");
            }

            String relevantData = similarDocuments.stream()
                    .map(Document::getText)
                    .collect(Collectors.joining(System.lineSeparator()));

            LOGGER.info("Response from vectorStore: {}", relevantData);
            return new AIChatResponse(relevantData);
        } catch (Exception e) {
            // Handling potential exceptions from the similarity search
            LOGGER.error("An error occurred during the similarity search: ", e);
            return new AIChatResponse("An error occurred while processing your request. Please try again later.");
        }
    }
}
