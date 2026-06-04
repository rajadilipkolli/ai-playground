package com.learning.ai.llmragwithspringai.rag.retrieval;

import com.learning.ai.llmragwithspringai.rag.join.RRFDocumentJoiner;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;

public class HybridDocumentRetriever implements DocumentRetriever {

    private static final Logger log = LoggerFactory.getLogger(HybridDocumentRetriever.class);
    private final VectorStoreDocumentRetriever vectorRetriever;
    private final KeywordDocumentRetriever keywordRetriever;
    private final RRFDocumentJoiner documentJoiner;
    private final Executor executor;

    public HybridDocumentRetriever(
            VectorStoreDocumentRetriever vectorRetriever,
            KeywordDocumentRetriever keywordRetriever,
            RRFDocumentJoiner documentJoiner,
            Executor executor) {
        this.vectorRetriever = vectorRetriever;
        this.keywordRetriever = keywordRetriever;
        this.documentJoiner = documentJoiner;
        this.executor = executor;
    }

    @Override
    public List<Document> retrieve(Query query) {
        log.debug("Executing hybrid retrieval for query: {}", query.text());

        CompletableFuture<List<Document>> vectorFuture = CompletableFuture.supplyAsync(
                () -> {
                    List<Document> docs = vectorRetriever.retrieve(query);
                    return docs.stream()
                            .map(d -> Document.builder()
                                    .id(d.getId())
                                    .text(d.getText())
                                    .media(d.getMedia())
                                    .metadata(d.getMetadata())
                                    .metadata("retrieval_source", "vector")
                                    .build())
                            .collect(Collectors.toList());
                },
                executor);

        CompletableFuture<List<Document>> keywordFuture = CompletableFuture.supplyAsync(
                () -> {
                    List<Document> docs = keywordRetriever.retrieve(query);
                    return docs.stream()
                            .map(d -> Document.builder()
                                    .id(d.getId())
                                    .text(d.getText())
                                    .media(d.getMedia())
                                    .metadata(d.getMetadata())
                                    .metadata("retrieval_source", "keyword")
                                    .build())
                            .collect(Collectors.toList());
                },
                executor);

        List<Document> vectorDocs = vectorFuture.join();
        List<Document> keywordDocs = keywordFuture.join();

        log.debug("Retrieved {} vector docs and {} keyword docs", vectorDocs.size(), keywordDocs.size());

        List<List<Document>> allDocs = new ArrayList<>();
        if (vectorDocs != null && !vectorDocs.isEmpty()) {
            allDocs.add(vectorDocs);
        }
        if (keywordDocs != null && !keywordDocs.isEmpty()) {
            allDocs.add(keywordDocs);
        }

        Map<Query, List<List<Document>>> joinerInput = new HashMap<>();
        joinerInput.put(query, allDocs);

        return documentJoiner.join(joinerInput);
    }
}
