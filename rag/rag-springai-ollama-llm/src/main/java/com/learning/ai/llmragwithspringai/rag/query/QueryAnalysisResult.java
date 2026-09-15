package com.learning.ai.llmragwithspringai.rag.query;

import java.util.Map;

public record QueryAnalysisResult(String cleanedQuery, Map<String, Object> filters) {}
