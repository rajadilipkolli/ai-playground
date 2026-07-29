package com.learning.ai.llmragwithspringai.rag.retrieval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

class CachingDocumentRetrieverTest {

    private DocumentRetriever delegate;
    private CacheManager cacheManager;
    private CachingDocumentRetriever retriever;

    @BeforeEach
    void setUp() {
        delegate = mock(DocumentRetriever.class);
        cacheManager = new ConcurrentMapCacheManager("retrieval-cache");
        retriever = new CachingDocumentRetriever(delegate, cacheManager, new SimpleMeterRegistry());
    }

    @Test
    void shouldCacheAndReturnFromCacheOnSubsequentCalls() {
        Query query = new Query("test cache");
        List<Document> docs = List.of(new Document("test"));
        when(delegate.retrieve(any())).thenReturn(docs);

        List<Document> result1 = retriever.retrieve(query);
        List<Document> result2 = retriever.retrieve(query);

        assertThat(result1).isEqualTo(docs);
        assertThat(result2).isEqualTo(docs);
        verify(delegate, times(1)).retrieve(any());
    }

    @Test
    void shouldConsiderFilterContextInCacheKey() {
        Query query = new Query("test cache");
        List<Document> docs1 = List.of(new Document("test1"));
        List<Document> docs2 = List.of(new Document("test2"));

        when(delegate.retrieve(any())).thenReturn(docs1, docs2);

        ScopedValue.where(
                        FilterContext.FILTER_EXPRESSION,
                        new org.springframework.ai.vectorstore.filter.FilterExpressionBuilder()
                                .eq("type", "A")
                                .build())
                .run(() -> retriever.retrieve(query));

        ScopedValue.where(
                        FilterContext.FILTER_EXPRESSION,
                        new org.springframework.ai.vectorstore.filter.FilterExpressionBuilder()
                                .eq("type", "B")
                                .build())
                .run(() -> retriever.retrieve(query));

        verify(delegate, times(2)).retrieve(any());
    }
}
