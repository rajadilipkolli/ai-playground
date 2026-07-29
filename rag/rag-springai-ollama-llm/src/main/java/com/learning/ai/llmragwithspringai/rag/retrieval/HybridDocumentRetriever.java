package com.learning.ai.llmragwithspringai.rag.retrieval;

import com.learning.ai.llmragwithspringai.rag.join.RRFDocumentJoiner;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.vectorstore.filter.Filter;

public class HybridDocumentRetriever implements DocumentRetriever {

    private static final Logger log = LoggerFactory.getLogger(HybridDocumentRetriever.class);
    private final DocumentRetriever vectorRetriever;
    private final KeywordDocumentRetriever keywordRetriever;
    private final RRFDocumentJoiner documentJoiner;
    private final Executor executor;

    public HybridDocumentRetriever(
            DocumentRetriever vectorRetriever,
            KeywordDocumentRetriever keywordRetriever,
            RRFDocumentJoiner documentJoiner,
            Executor executor) {
        this.vectorRetriever = vectorRetriever;
        this.keywordRetriever = keywordRetriever;
        this.documentJoiner = documentJoiner;
        this.executor = executor;
    }

    @Override
    public List<Document> retrieve(@NonNull Query query) {
        log.debug("Executing hybrid retrieval for query: {}", query.text());
        final Filter.Expression safeFilter = FilterContext.getFilterExpression();

        CompletableFuture<List<Document>> vectorFuture = CompletableFuture.supplyAsync(
                        () -> {
                            try {
                                return executeWithFilter(
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
                                        safeFilter);
                            } catch (RuntimeException | Error e) {
                                throw e;
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        },
                        executor)
                .exceptionally(ex -> {
                    log.warn("Vector retrieval failed, continuing with keyword results", ex);
                    return List.of();
                });

        CompletableFuture<List<Document>> keywordFuture = CompletableFuture.supplyAsync(
                        () -> {
                            try {
                                return executeWithFilter(
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
                                        safeFilter);
                            } catch (RuntimeException | Error e) {
                                throw e;
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        },
                        executor)
                .exceptionally(ex -> {
                    log.warn("Keyword retrieval failed, continuing with vector results", ex);
                    return List.of();
                });

        List<Document> vectorDocs = vectorFuture.join();
        List<Document> keywordDocs = keywordFuture.join();

        log.debug("Retrieved {} vector docs and {} keyword docs", vectorDocs.size(), keywordDocs.size());

        List<List<Document>> allDocs = new ArrayList<>();
        if (!vectorDocs.isEmpty()) {
            allDocs.add(vectorDocs);
        }
        if (!keywordDocs.isEmpty()) {
            allDocs.add(keywordDocs);
        }

        Map<Query, List<List<Document>>> joinerInput = new HashMap<>();
        joinerInput.put(query, allDocs);

        return documentJoiner.join(joinerInput);
    }

    private List<Document> executeWithFilter(Callable<List<Document>> task, Filter.Expression filter) throws Exception {
        if (filter != null) {
            return ScopedValue.where(FilterContext.FILTER_EXPRESSION, filter).call(task::call);
        } else {
            return task.call();
        }
    }
}
