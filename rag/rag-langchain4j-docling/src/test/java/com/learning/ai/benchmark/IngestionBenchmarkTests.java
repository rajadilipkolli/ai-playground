package com.learning.ai.benchmark;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

class IngestionBenchmarkTests {

    /** Verifies that overlapping batches maintain independent throughput counts. */
    @Test
    void calculatesThroughputFromCompletedDocumentsForEachOverlappingBatch() {
        IngestionBenchmark benchmark = new IngestionBenchmark(new SimpleMeterRegistry());
        benchmark.startBatch("first", 10);
        benchmark.startBatch("second", 10);

        benchmark.recordDocumentProcessed("first", 20);
        benchmark.recordDocumentProcessed("second", 20);
        benchmark.recordDocumentProcessed("first", 20);

        IngestionBenchmark.BenchmarkResult first = benchmark.getResults().get("first");
        IngestionBenchmark.BenchmarkResult second = benchmark.getResults().get("second");
        first.endTime = first.startTime + 1_000;
        second.endTime = second.startTime + 1_000;
        first.calculateMetrics();
        second.calculateMetrics();

        assertThat(first.getProcessedDocuments()).isEqualTo(2);
        assertThat(first.docsPerSecond).isEqualTo(2.0);
        assertThat(second.getProcessedDocuments()).isEqualTo(1);
        assertThat(second.docsPerSecond).isEqualTo(1.0);
    }
}
