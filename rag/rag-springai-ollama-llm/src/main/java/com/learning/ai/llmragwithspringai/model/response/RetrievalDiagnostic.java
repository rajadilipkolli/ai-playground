package com.learning.ai.llmragwithspringai.model.response;

public record RetrievalDiagnostic(String text, Double originalScore, Double rrfScore, String source) {}
