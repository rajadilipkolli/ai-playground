package com.learning.ai.config;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class VectorStoreComponent {

    private final VectorStore vectorStore;

    public VectorStoreComponent(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public void storeAndRetrieveEmbeddings() {
    // Store embeddings
    List<Document> documents =
            List.of(new Document("I like Spring Boot"),
                    new Document("I love Java programming language"));
        vectorStore.add(documents);

    // Retrieve embeddings
    SearchRequest query = SearchRequest.query("Spring Boot").withTopK(2);
    List<Document> similarDocuments = vectorStore.similaritySearch(query);
    String relevantData = similarDocuments.stream()
            .map(Document::getContent)
            .collect(Collectors.joining(System.lineSeparator()));
}
}
