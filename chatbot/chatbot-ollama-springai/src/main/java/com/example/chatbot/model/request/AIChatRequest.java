package com.example.chatbot.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AIChatRequest(
        @NotBlank(message = "Query cannot be empty") @Size(max = 800, message = "Query too long") @Pattern(regexp = "^[a-zA-Z0-9 ?.,!\\'\\-()]*$", message = "Invalid characters in query")
        String query) {}
