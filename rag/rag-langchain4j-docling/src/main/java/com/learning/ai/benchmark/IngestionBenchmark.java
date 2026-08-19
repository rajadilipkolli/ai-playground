package com.learning.ai.benchmark;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Service;

@Service
public class IngestionBenchmark {

    private final MeterRegistry meterRegistry;
    private final Map<String, BenchmarkResult> results = new ConcurrentHashMap<>();
    private final AtomicInteger processedDocuments = new AtomicInteger(0);

    public IngestionBenchmark(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public Timer getIngestionTimer() {
        return Timer.builder("rag.ingestion.latency")
                .description("Time taken to ingest a document")
                .register(meterRegistry);
    }

    public void recordDocumentProcessed(long timeInMillis) {
        processedDocuments.incrementAndGet();
        getIngestionTimer().record(java.time.Duration.ofMillis(timeInMillis));
    }

    public void startBatch(String batchId, int totalFiles) {
        results.put(batchId, new BenchmarkResult(batchId, totalFiles, System.currentTimeMillis()));
    }

    public void endBatch(String batchId) {
        BenchmarkResult result = results.get(batchId);
        if (result != null) {
            result.endTime = System.currentTimeMillis();
            result.calculateMetrics();
        }
    }

    public Map<String, BenchmarkResult> getResults() {
        return results;
    }

    public static class BenchmarkResult {
        public String batchId;
        public int totalFiles;
        public long startTime;
        public long endTime;
        public double docsPerSecond;
        public long memoryUsedBytes;

        public BenchmarkResult(String batchId, int totalFiles, long startTime) {
            this.batchId = batchId;
            this.totalFiles = totalFiles;
            this.startTime = startTime;
            Runtime rt = Runtime.getRuntime();
            this.memoryUsedBytes = rt.totalMemory() - rt.freeMemory();
        }

        public void calculateMetrics() {
            long durationMs = endTime - startTime;
            if (durationMs > 0) {
                this.docsPerSecond = (double) totalFiles / (durationMs / 1000.0);
            }
            Runtime rt = Runtime.getRuntime();
            long memoryAtEnd = rt.totalMemory() - rt.freeMemory();
            this.memoryUsedBytes = memoryAtEnd - this.memoryUsedBytes;
        }
    }
}
