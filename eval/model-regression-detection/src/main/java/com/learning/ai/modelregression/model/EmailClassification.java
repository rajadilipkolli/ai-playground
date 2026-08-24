package com.learning.ai.modelregression.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record EmailClassification(
        @NotNull Category category, @NotBlank String summary) {}
