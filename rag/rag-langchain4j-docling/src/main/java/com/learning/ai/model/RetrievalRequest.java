package com.learning.ai.model;

public record RetrievalRequest(
        String query,
        int maxResults,
        Double minScore,
        String elementType,
        String documentId,
        String sectionPathPrefix,
        Boolean hasTable
) {}
