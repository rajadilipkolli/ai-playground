package com.learning.ai.reactrag.model.response;

import java.util.List;

public record ChatResponse(String answer, List<String> retrievedDocuments, List<String> toolsUsed) {}
