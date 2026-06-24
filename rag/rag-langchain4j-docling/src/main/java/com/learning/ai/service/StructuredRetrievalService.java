package com.learning.ai.service;

import com.learning.ai.model.RetrievalMatch;
import com.learning.ai.model.RetrievalRequest;
import com.learning.ai.model.RetrievalResponse;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import com.learning.ai.benchmark.RetrievalBenchmark;

@Service
public class StructuredRetrievalService {

    private final EmbeddingStore<TextSegment> embeddingStore;
    private final EmbeddingModel embeddingModel;
    private final RetrievalBenchmark retrievalBenchmark;

    public StructuredRetrievalService(EmbeddingStore<TextSegment> embeddingStore, EmbeddingModel embeddingModel, RetrievalBenchmark retrievalBenchmark) {
        this.embeddingStore = embeddingStore;
        this.embeddingModel = embeddingModel;
        this.retrievalBenchmark = retrievalBenchmark;
    }

    public RetrievalResponse retrieve(RetrievalRequest request) {
        long startTime = System.currentTimeMillis();
        Embedding queryEmbedding = embeddingModel.embed(request.query()).content();

        Filter filter = buildFilter(request);

        EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(request.maxResults() > 0 ? request.maxResults() : 10)
                .minScore(request.minScore() != null ? request.minScore() : 0.0)
                .filter(filter)
                .build();

        dev.langchain4j.store.embedding.EmbeddingSearchResult<TextSegment> result = embeddingStore.search(searchRequest);

        List<RetrievalMatch> matches = result.matches().stream()
                .map(this::toMatchDTO)
                .collect(Collectors.toList());

        long duration = System.currentTimeMillis() - startTime;
        retrievalBenchmark.recordQuery(request.query(), duration, matches.size());

        return new RetrievalResponse(matches);
    }

    private Filter buildFilter(RetrievalRequest request) {
        List<Filter> filters = new ArrayList<>();

        if (request.elementType() != null && !request.elementType().isBlank()) {
            filters.add(MetadataFilterBuilder.metadataKey("element_type").isEqualTo(request.elementType()));
        }

        if (request.documentId() != null && !request.documentId().isBlank()) {
            filters.add(MetadataFilterBuilder.metadataKey("document_id").isEqualTo(request.documentId()));
        }

        // Simulating sectionPath prefix match - typically using equality or some other method, 
        // since exact prefix support varies by embedding store. We'll leave it out or implement a basic 'contains' if supported.
        // For standard pgvector JSONB, we'll try a strict equal or just omit prefix matching for this demo unless we want to do JSONB raw query.
        // Here, we just use equality for simplicity.
        if (request.sectionPathPrefix() != null && !request.sectionPathPrefix().isBlank()) {
            filters.add(MetadataFilterBuilder.metadataKey("section_path").isEqualTo(request.sectionPathPrefix()));
        }

        if (Boolean.TRUE.equals(request.hasTable())) {
            filters.add(MetadataFilterBuilder.metadataKey("element_type").isEqualTo("table"));
        }

        if (filters.isEmpty()) {
            return null;
        }

        Filter combinedFilter = filters.get(0);
        for (int i = 1; i < filters.size(); i++) {
            combinedFilter = combinedFilter.and(filters.get(i));
        }

        return combinedFilter;
    }

    private RetrievalMatch toMatchDTO(EmbeddingMatch<TextSegment> match) {
        return new RetrievalMatch(
                match.embedded() != null ? match.embedded().text() : null,
                match.score(),
                match.embedded() != null ? match.embedded().metadata().toMap() : null
        );
    }
}
