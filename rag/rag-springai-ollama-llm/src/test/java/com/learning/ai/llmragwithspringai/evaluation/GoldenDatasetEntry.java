package com.learning.ai.llmragwithspringai.evaluation;

import java.util.List;

public record GoldenDatasetEntry(
        String question, List<String> expectedAnswerKeywords, List<String> expectedContextKeywords) {}
