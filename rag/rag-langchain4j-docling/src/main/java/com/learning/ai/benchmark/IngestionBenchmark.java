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

    /** Creates an ingestion benchmark backed by the supplied meter registry. */
    public IngestionBenchmark(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    /** Returns the timer used to measure document ingestion latency. */
    public Timer getIngestionTimer() {
        return Timer.builder("rag.ingestion.latency")
                .description("Time taken to ingest a document")
                .register(meterRegistry);
    }

    /** Records the processing time for one document. */
    public void recordDocumentProcessed(long timeInMillis) {
        processedDocuments.incrementAndGet();
        getIngestionTimer().record(java.time.Duration.ofMillis(timeInMillis));
    }

    /** Records global metrics and, when the batch exists, increments its processed-document count. */
    public void recordDocumentProcessed(String batchId, long timeInMillis) {
        recordDocumentProcessed(timeInMillis);
        BenchmarkResult result = results.get(batchId);
        if (result != null) {
            result.recordDocumentProcessed();
        }
    }

    /** Starts collecting metrics for a batch, replacing any result with the same identifier. */
    public void startBatch(String batchId, int totalFiles) {
        results.put(batchId, new BenchmarkResult(batchId, totalFiles, System.currentTimeMillis()));
    }

    /** Finishes a batch and calculates its aggregate metrics. */
    public void endBatch(String batchId) {
        BenchmarkResult result = results.get(batchId);
        if (result != null) {
            result.endTime = System.currentTimeMillis();
            result.calculateMetrics();
        }
    }

    /** Returns the live, mutable benchmark results keyed by batch identifier. */
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
        private final AtomicInteger processedDocuments = new AtomicInteger();

        /** Creates the mutable metrics for an ingestion batch. */
        public BenchmarkResult(String batchId, int totalFiles, long startTime) {
            this.batchId = batchId;
            this.totalFiles = totalFiles;
            this.startTime = startTime;
            Runtime rt = Runtime.getRuntime();
            this.memoryUsedBytes = rt.totalMemory() - rt.freeMemory();
        }

        /** Calculates throughput and memory usage for the completed batch. */
        public void calculateMetrics() {
            long durationMs = endTime - startTime;
            if (durationMs > 0) {
                this.docsPerSecond = (double) processedDocuments.get() / (durationMs / 1000.0);
            }
            Runtime rt = Runtime.getRuntime();
            long memoryAtEnd = rt.totalMemory() - rt.freeMemory();
            this.memoryUsedBytes = memoryAtEnd - this.memoryUsedBytes;
        }

        /** Returns the number of documents recorded for this batch. */
        public int getProcessedDocuments() {
            return processedDocuments.get();
        }

        /** Increments the processed-document count for this batch. */
        private void recordDocumentProcessed() {
            processedDocuments.incrementAndGet();
        }
    }
}
