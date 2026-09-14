package com.learning.ai.model;

import java.util.Map;

/** Represents one vector retrieval match and its metadata. */
public record RetrievalMatch(
        String text,
        Double score,
        Map<String, Object> metadata
) {}
