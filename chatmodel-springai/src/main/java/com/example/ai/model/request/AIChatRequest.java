package com.example.ai.model.request;

import jakarta.validation.constraints.NotBlank;

public record AIChatRequest(@NotBlank(message = "Query cant be Blank") String query) {}
