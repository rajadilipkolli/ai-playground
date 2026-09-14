package com.learning.ai.modelregression.storage;

import java.time.LocalDateTime;
import java.util.List;

public record EvaluationRun(
        String runId,
        LocalDateTime timestamp,
        String promptVersion,
        String datasetVersion,
        String model,
        double passRatePercent,
        double averageLatencyMs,
        int totalTokens,
        List<CaseResult> caseResults) {}
