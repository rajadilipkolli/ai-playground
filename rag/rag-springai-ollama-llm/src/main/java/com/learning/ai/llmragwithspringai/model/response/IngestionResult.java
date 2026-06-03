package com.learning.ai.llmragwithspringai.model.response;

public record IngestionResult(String status, String filename, int chunksIngested, int chunksDeleted) {}
