package com.learning.ai.llmragwithspringai.rag.retrieval;

import com.learning.ai.llmragwithspringai.util.ContentHashUtil;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.pgvector.PgVectorFilterExpressionConverter;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

public class CachingDocumentRetriever implements DocumentRetriever {

    private static final Logger log = LoggerFactory.getLogger(CachingDocumentRetriever.class);

    private static final PgVectorFilterExpressionConverter VECTOR_FILTER_EXPRESSION_CONVERTER =
            new PgVectorFilterExpressionConverter();

    private final DocumentRetriever delegate;
    private final CacheManager cacheManager;
    private final Counter hitsCounter;
    private final Counter missesCounter;

    public CachingDocumentRetriever(
            DocumentRetriever delegate, CacheManager cacheManager, MeterRegistry meterRegistry) {
        this.delegate = delegate;
        this.cacheManager = cacheManager;
        this.hitsCounter = meterRegistry.counter("rag.cache.hits");
        this.missesCounter = meterRegistry.counter("rag.cache.misses");
    }

    @Override
    public List<Document> retrieve(@NonNull Query query) {
        Filter.Expression filter = FilterContext.getFilterExpression();
        String filterString = "";
        if (filter != null) {
            filterString = VECTOR_FILTER_EXPRESSION_CONVERTER.convertExpression(filter);
        }
        String rawKey = "query:" + query.text() + "|filter:" + filterString;
        String cacheKey = ContentHashUtil.getSha256Hash(rawKey);

        Cache cache = cacheManager.getCache("retrieval-cache");
        if (cache != null) {
            Cache.ValueWrapper wrapper = cache.get(cacheKey);
            if (wrapper != null) {
                Object cachedValue = wrapper.get();
                if (cachedValue instanceof List<?> list) {
                    log.debug("Cache hit for query: {}", query.text());
                    hitsCounter.increment();
                    return (List<Document>) list;
                }
            }
        }

        log.debug("Cache miss for query: {}", query.text());
        missesCounter.increment();
        List<Document> documents = delegate.retrieve(query);

        if (cache != null && documents != null && !documents.isEmpty()) {
            cache.put(cacheKey, documents);
        }

        return documents;
    }
}
