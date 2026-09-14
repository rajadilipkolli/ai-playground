package com.learning.ai.model;

import java.util.List;

/** Contains the matches returned by a retrieval query. */
public record RetrievalResponse(
        List<RetrievalMatch> matches
) {}
