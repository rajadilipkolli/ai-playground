package com.learning.ai.model;

import java.util.List;

public record RetrievalResponse(
        List<RetrievalMatch> matches
) {}
