package com.learning.ai.modelregression.model;

import jakarta.validation.constraints.NotBlank;

public record PromptConfig(
        @NotBlank String version,
        @NotBlank String timestamp,
        @NotBlank String systemPrompt,
        @NotBlank String fewShotExamples) {}
