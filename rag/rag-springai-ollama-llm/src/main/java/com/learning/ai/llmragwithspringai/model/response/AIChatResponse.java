package com.learning.ai.llmragwithspringai.model.response;

import java.util.List;

public record AIChatResponse(String queryResponse, List<RetrievalDiagnostic> diagnostics) {
    public AIChatResponse(String queryResponse) {
        this(queryResponse, null);
    }
}
