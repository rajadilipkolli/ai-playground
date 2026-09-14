package com.learning.ai.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class IngestionJobTests {

    /** Verifies that concurrent counter updates are not lost. */
    @Test
    void updatesCountersAtomically() {
        IngestionJob job = new IngestionJob("job", 2_000);

        IntStream.range(0, 1_000).parallel().forEach(i -> {
            job.incrementProcessed();
            job.incrementFailed();
        });

        assertThat(job.getProcessedFiles()).isEqualTo(1_000);
        assertThat(job.getFailedFiles()).isEqualTo(1_000);
        assertThat(job.getTotalFiles()).isEqualTo(2_000);
    }
}
