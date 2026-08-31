package com.learning.ai.model;

import java.util.Map;

public record RetrievalMatch(
        String text,
        Double score,
        Map<String, Object> metadata
) {}
