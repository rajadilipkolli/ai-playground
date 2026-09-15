package com.learning.ai.model;

/** Describes a vector retrieval query and its optional metadata filters. */
public record RetrievalRequest(
        String query,
        int maxResults,
        Double minScore,
        String elementType,
        String documentId,
        String sectionPathExact,
        Boolean hasTable) {}
