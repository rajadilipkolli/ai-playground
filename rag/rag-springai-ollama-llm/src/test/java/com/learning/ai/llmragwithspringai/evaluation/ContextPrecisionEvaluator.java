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

public class ContextPrecisionEvaluator implements Evaluator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContextPrecisionEvaluator.class);
    private final ChatClient chatClient;
    private final double passThreshold;

    public ContextPrecisionEvaluator(ChatClient.Builder chatClientBuilder) {
        this(chatClientBuilder, 0.2);
    }

    public ContextPrecisionEvaluator(ChatClient.Builder chatClientBuilder, double passThreshold) {
        this.chatClient = chatClientBuilder.build();
        this.passThreshold = passThreshold;
    }

    @Override
    public EvaluationResponse evaluate(EvaluationRequest request) {
        String question = request.getUserText();
        List<Document> contextDocs = request.getDataList();

        if (contextDocs == null || contextDocs.isEmpty()) {
            LOGGER.info("ContextPrecisionEvaluator - No context provided. Score: 0.0, Pass: false");
            return new EvaluationResponse(false, 0.0f, "Context Precision", Map.of());
        }

        String context = String.join(
                "\n\n---\n\n", contextDocs.stream().map(Document::getText).toList());

        String prompt = """
                You are an expert evaluator. Your task is to evaluate context precision.

                Question: {question}

                Retrieved Contexts (separated by ---):
                {context}

                Step 1: For each retrieved context chunk, assess if it is relevant and useful for answering the Question.
                Step 2: Return your evaluation as a JSON list of booleans, where true means the chunk is relevant, and false means it is not.
                The number of booleans must exactly match the number of context chunks provided.

                Respond ONLY with the JSON array of booleans. Do not include markdown tags or any other text.
                """;

        String jsonResponse = chatClient
                .prompt()
                .user(u -> u.text(prompt).param("question", question).param("context", context))
                .call()
                .content();

        long trueCount = countOccurrences(jsonResponse.toLowerCase(), "true");
        long falseCount = countOccurrences(jsonResponse.toLowerCase(), "false");
        long totalChunks = trueCount + falseCount;

        // If the LLM didn't return a clear list matching the size, we fallback to our best count
        if (totalChunks == 0) {
            totalChunks = contextDocs.size();
        }

        double score = (double) trueCount / totalChunks;
        boolean pass = score >= passThreshold;

        LOGGER.info("ContextPrecisionEvaluator - Score: {}, Pass: {}, Details: {}", score, pass, jsonResponse);

        return new EvaluationResponse(pass, (float) score, "Context Precision", Map.of());
    }

    private long countOccurrences(String str, String word) {
        return (str.length() - str.replace(word, "").length()) / word.length();
    }
}
