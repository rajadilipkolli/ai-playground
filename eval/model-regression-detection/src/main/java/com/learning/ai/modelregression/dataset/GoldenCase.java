package com.learning.ai.modelregression.dataset;

import com.learning.ai.modelregression.model.Category;

public record GoldenCase(
        String id,
        String email,
        Category expectedCategory,
        String expectedSummary,
        String expectedDifficulty,
        String notes) {}
