package com.learning.ai.llmragwithspringai.model.response;

import com.fasterxml.jackson.annotation.JsonValue;

public enum IngestionStatus {
    INGESTED("ingested"),
    SKIPPED_DUPLICATE("skipped_duplicate"),
    REPLACED("replaced");

    private final String value;

    IngestionStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
