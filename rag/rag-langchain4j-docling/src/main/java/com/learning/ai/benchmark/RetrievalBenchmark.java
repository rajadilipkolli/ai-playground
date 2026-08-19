package com.learning.ai.benchmark;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class RetrievalBenchmark {

    private final MeterRegistry meterRegistry;
    private final Map<String, QueryResult> results = new ConcurrentHashMap<>();

    public RetrievalBenchmark(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public Timer getRetrievalTimer() {
        return Timer.builder("rag.retrieval.latency")
                .description("Time taken for a retrieval query")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry);
    }

    public void recordQuery(String query, long timeInMillis, int returnedMatches) {
        getRetrievalTimer().record(java.time.Duration.ofMillis(timeInMillis));
        results.put(query, new QueryResult(query, timeInMillis, returnedMatches));
    }

    public Map<String, QueryResult> getResults() {
        return results;
    }

    public static class QueryResult {
        public String query;
        public long latencyMs;
        public int matchesCount;

        public QueryResult(String query, long latencyMs, int matchesCount) {
            this.query = query;
            this.latencyMs = latencyMs;
            this.matchesCount = matchesCount;
        }
    }
}
