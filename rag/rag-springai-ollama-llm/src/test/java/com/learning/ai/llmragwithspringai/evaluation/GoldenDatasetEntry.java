package com.learning.ai.llmragwithspringai.evaluation;

import java.util.List;

/**
 * Represents a single entry in the golden evaluation dataset.
 * Each entry contains a question and expected keywords for answer and context validation.
 */
public record GoldenDatasetEntry(
        String question, List<String> expectedAnswerKeywords, List<String> expectedContextKeywords) {}
