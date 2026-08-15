package com.learning.ai.modelregression.model;

import jakarta.validation.constraints.NotBlank;

public record EmailRequest(
        @NotBlank(message = "Email text must not be blank") String emailText) {}
