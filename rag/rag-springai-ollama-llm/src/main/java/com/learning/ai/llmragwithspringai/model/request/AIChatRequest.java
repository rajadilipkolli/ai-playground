package com.learning.ai.llmragwithspringai.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.io.Serializable;

public record AIChatRequest(
        @NotBlank(message = "Query cannot be empty") @Size(max = 800, message = "Query exceeds maximum length") @Pattern(regexp = "^[a-zA-Z0-9 ?.,!'-]*$", message = "Invalid characters in query") String question,

        @Size(max = 100, message = "Document type exceeds maximum length") @Pattern(regexp = "^[a-zA-Z0-9_-]*$", message = "Invalid characters in document type") String documentType,
        @Size(max = 100, message = "Owner exceeds maximum length") @Pattern(regexp = "^[a-zA-Z0-9_-]*$", message = "Invalid characters in owner") String owner,
        @Size(max = 100, message = "Category exceeds maximum length") @Pattern(regexp = "^[a-zA-Z0-9_-]*$", message = "Invalid characters in category") String category)
        implements Serializable {}
