package com.learning.ai.reactrag.model.response;

import java.util.List;

public record IngestionResponse(int chunkCount, List<String> errors) {}
