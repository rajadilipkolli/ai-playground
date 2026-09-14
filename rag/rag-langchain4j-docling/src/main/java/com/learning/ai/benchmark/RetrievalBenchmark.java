package com.learning.ai.benchmark;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class RetrievalBenchmark {

    private final Timer retrievalTimer;
    private final Map<String, QueryResult> results = new ConcurrentHashMap<>();

    public RetrievalBenchmark(MeterRegistry meterRegistry) {
        this.retrievalTimer = Timer.builder("rag.retrieval.latency")
                .description("Time taken for a retrieval query")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry);
    }

    public Timer getRetrievalTimer() {
        return retrievalTimer;
    }

    public void recordQuery(String query, long timeInMillis, int returnedMatches) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("Query must not be null or blank");
        }
        getRetrievalTimer().record(java.time.Duration.ofMillis(timeInMillis));
        results.put(query, new QueryResult(query, timeInMillis, returnedMatches));
    }

    public Map<String, QueryResult> getResults() {
        return Collections.unmodifiableMap(results);
    }

    public record QueryResult(String query, long latencyMs, int matchesCount) {}
}
