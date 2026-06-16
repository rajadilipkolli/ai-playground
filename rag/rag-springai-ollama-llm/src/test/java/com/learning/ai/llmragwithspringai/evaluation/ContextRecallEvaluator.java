package com.learning.ai.llmragwithspringai.evaluation;

import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.evaluation.EvaluationRequest;
import org.springframework.ai.evaluation.EvaluationResponse;
import org.springframework.ai.evaluation.Evaluator;

public class ContextRecallEvaluator implements Evaluator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContextRecallEvaluator.class);
    private final ChatClient chatClient;
    private final double passThreshold;

    public ContextRecallEvaluator(ChatClient.Builder chatClientBuilder) {
        this(chatClientBuilder, 0.7);
    }

    public ContextRecallEvaluator(ChatClient.Builder chatClientBuilder, double passThreshold) {
        this.chatClient = chatClientBuilder.build();
        this.passThreshold = passThreshold;
    }

    @Override
    public EvaluationResponse evaluate(EvaluationRequest request) {
        String groundTruth = null;
        if (request instanceof RagasEvaluationRequest r) {
            groundTruth = r.getGroundTruthAnswer();
        }

        if (groundTruth == null || groundTruth.isBlank()) {
            LOGGER.info("ContextRecallEvaluator - No ground truth provided, skipping evaluation.");
            return new EvaluationResponse(true, -1.0f, "Context Recall (N/A)", Map.of());
        }

        List<Document> contextDocs = request.getDataList();
        String context = contextDocs == null
                ? ""
                : String.join(
                        "\n\n", contextDocs.stream().map(Document::getText).toList());

        String prompt = """
                You are an expert evaluator. Your task is to evaluate context recall.

                Ground Truth Answer: {groundTruth}

                Retrieved Context:
                {context}

                Step 1: Identify key factual statements in the Ground Truth Answer.
                Step 2: Check if each statement is covered by the Retrieved Context.
                Step 3: Return your evaluation as a JSON list of booleans, where true means the statement is covered by the context, and false means it is not.

                Respond ONLY with the JSON array of booleans. Do not include markdown tags or any other text.
                """;

        final String finalGroundTruth = groundTruth;
        String jsonResponse = chatClient
                .prompt()
                .user(u -> u.text(prompt).param("groundTruth", finalGroundTruth).param("context", context))
                .call()
                .content();

        long trueCount = countOccurrences(jsonResponse.toLowerCase(), "true");
        long falseCount = countOccurrences(jsonResponse.toLowerCase(), "false");
        long totalStatements = trueCount + falseCount;

        double score = totalStatements == 0 ? 0.0 : (double) trueCount / totalStatements;
        boolean pass = score >= passThreshold;

        LOGGER.info("ContextRecallEvaluator - Score: {}, Pass: {}, Details: {}", score, pass, jsonResponse);

        return new EvaluationResponse(pass, (float) score, "Context Recall", Map.of());
    }

    private long countOccurrences(String str, String word) {
        return (str.length() - str.replace(word, "").length()) / word.length();
    }
}
