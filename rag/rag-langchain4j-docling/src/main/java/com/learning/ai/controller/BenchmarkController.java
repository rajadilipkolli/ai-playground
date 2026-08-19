package com.learning.ai.controller;

import com.learning.ai.benchmark.IngestionBenchmark;
import com.learning.ai.benchmark.RetrievalBenchmark;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/benchmark")
public class BenchmarkController {

    private final IngestionBenchmark ingestionBenchmark;
    private final RetrievalBenchmark retrievalBenchmark;

    public BenchmarkController(IngestionBenchmark ingestionBenchmark, RetrievalBenchmark retrievalBenchmark) {
        this.ingestionBenchmark = ingestionBenchmark;
        this.retrievalBenchmark = retrievalBenchmark;
    }

    @GetMapping("/results")
    public ResponseEntity<Map<String, Object>> getResults() {
        Map<String, Object> results = new HashMap<>();
        results.put("ingestion", ingestionBenchmark.getResults());
        results.put("retrieval", retrievalBenchmark.getResults());
        return ResponseEntity.ok(results);
    }
}
