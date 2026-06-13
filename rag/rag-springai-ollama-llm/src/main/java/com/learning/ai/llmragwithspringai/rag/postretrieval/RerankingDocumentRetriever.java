package com.learning.ai.llmragwithspringai.rag.postretrieval;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.List;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;

public class RerankingDocumentRetriever implements DocumentRetriever {

    private final DocumentRetriever delegate;
    private final RelevanceDocumentReranker reranker;
    private final MeterRegistry meterRegistry;

    public RerankingDocumentRetriever(
            DocumentRetriever delegate, RelevanceDocumentReranker reranker, MeterRegistry meterRegistry) {
        this.delegate = delegate;
        this.reranker = reranker;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public List<Document> retrieve(Query query) {
        List<Document> retrievedDocs = delegate.retrieve(query);

        Timer timer = meterRegistry.timer("rag.rerank.latency");
        return timer.record(() -> reranker.rerank(retrievedDocs, query));
    }
}
