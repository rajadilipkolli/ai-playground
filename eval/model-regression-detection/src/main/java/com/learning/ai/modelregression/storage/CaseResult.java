package com.learning.ai.modelregression.storage;

public record CaseResult(
        String caseId,
        boolean categoryMatch,
        int relevanceScore,
        long latencyMs,
        int tokens,
        boolean pass,
        // Add additional fields for reporting
        String originalEmail,
        String expectedCategory,
        String expectedSummary,
        String actualCategory,
        String actualSummary) {}
