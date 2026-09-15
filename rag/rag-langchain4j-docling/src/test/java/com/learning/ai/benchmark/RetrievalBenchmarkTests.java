package com.learning.ai.benchmark;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

class RetrievalBenchmarkTests {

    /** Verifies timer reuse and immutable exposure of recorded results. */
    @Test
    void reusesTimerAndExposesImmutableResults() {
        RetrievalBenchmark benchmark = new RetrievalBenchmark(new SimpleMeterRegistry());

        assertThat(benchmark.getRetrievalTimer()).isSameAs(benchmark.getRetrievalTimer());
        benchmark.recordQuery("query", 12, 3);

        assertThat(benchmark.getResults().get("query")).isEqualTo(new RetrievalBenchmark.QueryResult("query", 12, 3));
        assertThatThrownBy(() -> benchmark.getResults().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    /** Verifies that blank queries are rejected before metrics are recorded. */
    @Test
    void rejectsBlankQueriesBeforeRecording() {
        RetrievalBenchmark benchmark = new RetrievalBenchmark(new SimpleMeterRegistry());

        assertThatThrownBy(() -> benchmark.recordQuery(" ", 12, 3))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Query must not be null or blank");
        assertThat(benchmark.getRetrievalTimer().count()).isZero();
        assertThat(benchmark.getResults()).isEmpty();
    }
}
