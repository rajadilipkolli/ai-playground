package com.learning.ai.llmragwithspringai.rag.retrieval;

import com.learning.ai.llmragwithspringai.util.ContentHashUtil;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

public class CachingDocumentRetriever implements DocumentRetriever {

    private static final Logger log = LoggerFactory.getLogger(CachingDocumentRetriever.class);
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
    public List<Document> retrieve(Query query) {
        String queryText = query.text();
        String filterExp = FilterContext.getFilterExpression() != null ? FilterContext.getFilterExpression() : "";
        String rawKey = queryText + "::" + filterExp;
        String cacheKey = ContentHashUtil.getSha256Hash(rawKey);

        Cache cache = cacheManager.getCache("retrieval-cache");
        if (cache != null) {
            Cache.ValueWrapper wrapper = cache.get(cacheKey);
            if (wrapper != null) {
                log.debug("Cache hit for query: {}", queryText);
                hitsCounter.increment();
                return (List<Document>) wrapper.get();
            }
        }

        log.debug("Cache miss for query: {}", queryText);
        missesCounter.increment();
        List<Document> documents = delegate.retrieve(query);

        if (cache != null && documents != null && !documents.isEmpty()) {
            cache.put(cacheKey, documents);
        }

        return documents;
    }
}
