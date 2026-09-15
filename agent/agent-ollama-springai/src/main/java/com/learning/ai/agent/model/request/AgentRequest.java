package com.learning.ai.agent.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AgentRequest(
        @NotBlank(message = "Message must not be blank")
        @Size(min = 1, max = 2000, message = "Message must be between 1 and 2000 characters")
        String message) {}
