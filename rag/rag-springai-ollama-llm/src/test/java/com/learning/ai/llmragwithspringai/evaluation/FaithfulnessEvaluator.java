package com.learning.ai.llmragwithspringai.evaluation;

import java.util.List;
import java.util.Map;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.evaluation.EvaluationRequest;
import org.springframework.ai.evaluation.EvaluationResponse;
import org.springframework.ai.evaluation.Evaluator;

public class FaithfulnessEvaluator implements Evaluator {

    private static final Logger LOGGER = LoggerFactory.getLogger(FaithfulnessEvaluator.class);
    private final ChatClient chatClient;
    private final double passThreshold;

    public FaithfulnessEvaluator(ChatClient.Builder chatClientBuilder) {
        this(chatClientBuilder, 0.5);
    }

    public FaithfulnessEvaluator(ChatClient.Builder chatClientBuilder, double passThreshold) {
        this.chatClient = chatClientBuilder.build();
        this.passThreshold = passThreshold;
    }

    @Override
    public EvaluationResponse evaluate(@NonNull EvaluationRequest request) {
        String answer = "";
        if (request instanceof RagasEvaluationRequest r) {
            answer = r.getAnswer();
        }
        List<Document> contextDocs = request.getDataList();
        String context =
                String.join("\n\n", contextDocs.stream().map(Document::getText).toList());

        String prompt = """
                You are an expert evaluator. Your task is to evaluate the faithfulness of an answer given a context.

                Answer: {answer}

                Context: {context}

                Step 1: Decompose the Answer into distinct factual claims.
                Step 2: For each claim, check if it is supported by the Context.
                Step 3: Return your evaluation as a JSON list of booleans, where true means the claim is supported by the context, and false means it is not.
                Example Output: [true, false, true]

                Respond ONLY with the JSON array of booleans. Do not include markdown tags or any other text.
                """;

        final String finalAnswer = answer;
        String jsonResponse = chatClient
                .prompt()
                .user(u -> u.text(prompt).param("answer", finalAnswer).param("context", context))
                .call()
                .content();

        if (jsonResponse == null || jsonResponse.isBlank()) {
            return new EvaluationResponse(false, 0.0f, "Faithfulness", Map.of());
        }
        long trueCount = countOccurrences(jsonResponse.toLowerCase(), "true");
        long falseCount = countOccurrences(jsonResponse.toLowerCase(), "false");
        long totalClaims = trueCount + falseCount;

        double score = totalClaims == 0 ? 0.0 : (double) trueCount / totalClaims;
        boolean pass = score >= passThreshold;

        LOGGER.info("FaithfulnessEvaluator - Score: {}, Pass: {}, Details: {}", score, pass, jsonResponse);

        return new EvaluationResponse(pass, (float) score, "Faithfulness", Map.of());
    }

    private long countOccurrences(String str, String word) {
        return (str.length() - str.replace(word, "").length()) / word.length();
    }
}
