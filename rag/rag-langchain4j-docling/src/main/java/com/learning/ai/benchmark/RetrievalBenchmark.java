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

    /** Creates retrieval metrics backed by the supplied meter registry. */
    public RetrievalBenchmark(MeterRegistry meterRegistry) {
        this.retrievalTimer = Timer.builder("rag.retrieval.latency")
                .description("Time taken for a retrieval query")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry);
    }

    /** Returns the timer used to measure retrieval latency. */
    public Timer getRetrievalTimer() {
        return retrievalTimer;
    }

    /** Records the latency and match count for a query. */
    public void recordQuery(String query, long timeInMillis, int returnedMatches) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("Query must not be null or blank");
        }
        getRetrievalTimer().record(java.time.Duration.ofMillis(timeInMillis));
        results.put(query, new QueryResult(query, timeInMillis, returnedMatches));
    }

    /** Returns an immutable view of results keyed by query. */
    public Map<String, QueryResult> getResults() {
        return Collections.unmodifiableMap(results);
    }

    /** Captures the result metrics for one retrieval query. */
    public record QueryResult(String query, long latencyMs, int matchesCount) {}
}
