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
public class PgVectorStoreService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PgVectorStoreService.class);

    private final VectorStore vectorStore;

    public PgVectorStoreService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public void storeEmbeddings() {
        // Store embeddings
        List<Document> documents =
                List.of(new Document("I like football."), new Document("The weather is good today."));
        vectorStore.add(documents);
    }

    public AIChatResponse queryEmbeddingStore(String question) {
        // Retrieve embeddings
        SearchRequest query = SearchRequest.query(question).withTopK(1);
        List<Document> similarDocuments = vectorStore.similaritySearch(query);
        String relevantData =
                similarDocuments.stream().map(Document::getContent).collect(Collectors.joining(System.lineSeparator()));

        LOGGER.info("response from vectorStore : {} ", relevantData);
        return new AIChatResponse(relevantData);
    }
}
