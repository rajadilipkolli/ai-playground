package com.learning.ai.reactrag.model.response;

import java.util.List;

public record IngestionResponse(int documentCount, List<String> errors) {}
