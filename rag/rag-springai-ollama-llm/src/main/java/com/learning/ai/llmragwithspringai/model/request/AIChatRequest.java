package com.learning.ai.llmragwithspringai.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.util.Map;

public record AIChatRequest(
        @NotBlank(message = "Query cannot be empty")
        @Size(max = 1000, message = "Query exceeds maximum length")
        @Pattern(regexp = "^[\\p{L}\\p{N}\\p{P}\\p{Z}]*$", message = "Invalid characters in query")
        String question,

        @Size(max = 100, message = "Document type exceeds maximum length")
        @Pattern(regexp = "^[a-zA-Z0-9_-]*$", message = "Invalid characters in document type")
        String documentType,

        @Size(max = 100, message = "Owner exceeds maximum length")
        @Pattern(regexp = "^[a-zA-Z0-9_-]*$", message = "Invalid characters in owner")
        String owner,

        @Size(max = 100, message = "Category exceeds maximum length")
        @Pattern(regexp = "^[a-zA-Z0-9_-]*$", message = "Invalid characters in category")
        String category,

        @Size(max = 10, message = "Too many filters")
        Map<String, @Size(max = 50) @Pattern(regexp = "^[a-zA-Z0-9_-]*$") String> filters)
        implements Serializable {}
