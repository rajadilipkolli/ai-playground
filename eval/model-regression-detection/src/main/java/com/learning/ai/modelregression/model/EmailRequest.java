package com.learning.ai.modelregression.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EmailRequest(
        @NotBlank(message = "Email text must not be blank") @Size(max = 8000, message = "Email text must not exceed 8000 characters") String emailText) {}
