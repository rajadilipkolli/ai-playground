package com.learning.ai.llmragwithspringai.model.response;

public record IngestionResult(IngestionStatus status, String filename, int chunksIngested, int chunksDeleted) {}
