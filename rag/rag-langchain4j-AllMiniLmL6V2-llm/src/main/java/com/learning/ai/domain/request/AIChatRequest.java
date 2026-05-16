package com.learning.ai.domain.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.io.Serializable;

public record AIChatRequest(
        @NotBlank(message = "Query cannot be empty") @Size(max = 800, message = "Query exceeds maximum length") @Pattern(regexp = "^[a-zA-Z0-9 ?]*$", message = "Invalid characters in query") String question)
        implements Serializable {}
