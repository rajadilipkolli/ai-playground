package com.learning.ai.service;

import com.learning.ai.benchmark.RetrievalBenchmark;
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

@Service
public class StructuredRetrievalService {

    private final EmbeddingStore<TextSegment> embeddingStore;
    private final EmbeddingModel embeddingModel;
    private final RetrievalBenchmark retrievalBenchmark;

    /** Creates the retrieval service and its benchmark recorder. */
    public StructuredRetrievalService(
            EmbeddingStore<TextSegment> embeddingStore,
            EmbeddingModel embeddingModel,
            RetrievalBenchmark retrievalBenchmark) {
        this.embeddingStore = embeddingStore;
        this.embeddingModel = embeddingModel;
        this.retrievalBenchmark = retrievalBenchmark;
    }

    /**
     * Embeds a query, applies metadata filters, and returns matching segments. Uses ten results when the
     * requested maximum is not positive and a minimum score of zero when none is supplied.
     */
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

        dev.langchain4j.store.embedding.EmbeddingSearchResult<TextSegment> result =
                embeddingStore.search(searchRequest);

        List<RetrievalMatch> matches =
                result.matches().stream().map(this::toMatchDTO).collect(Collectors.toList());

        long duration = System.currentTimeMillis() - startTime;
        retrievalBenchmark.recordQuery(request.query(), duration, matches.size());

        return new RetrievalResponse(matches);
    }

    /** Builds the combined metadata filter requested by the caller. */
    private Filter buildFilter(RetrievalRequest request) {
        List<Filter> filters = new ArrayList<>();

        if (request.elementType() != null && !request.elementType().isBlank()) {
            filters.add(MetadataFilterBuilder.metadataKey("element_type").isEqualTo(request.elementType()));
        }

        if (request.documentId() != null && !request.documentId().isBlank()) {
            filters.add(MetadataFilterBuilder.metadataKey("document_id").isEqualTo(request.documentId()));
        }

        if (request.sectionPathExact() != null && !request.sectionPathExact().isBlank()) {
            filters.add(MetadataFilterBuilder.metadataKey("section_path").isEqualTo(request.sectionPathExact()));
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

    /** Converts an embedding-store match into the API response model. */
    private RetrievalMatch toMatchDTO(EmbeddingMatch<TextSegment> match) {
        return new RetrievalMatch(
                match.embedded() != null ? match.embedded().text() : null,
                match.score(),
                match.embedded() != null ? match.embedded().metadata().toMap() : null);
    }
}
