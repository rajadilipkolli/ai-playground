package com.learning.ai.llmragwithspringai.model.response;

import java.util.List;
import org.jspecify.annotations.Nullable;

public record AIChatResponse(String queryResponse, @Nullable List<RetrievalDiagnostic> diagnostics) {}
