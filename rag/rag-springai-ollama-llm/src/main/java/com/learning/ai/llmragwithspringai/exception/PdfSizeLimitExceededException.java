package com.learning.ai.llmragwithspringai.exception;

public class PdfSizeLimitExceededException extends RuntimeException {
    public PdfSizeLimitExceededException(String message) {
        super(message);
    }
}
