package com.learning.ai.llmragwithspringai.evaluation;

import java.util.List;
import org.springframework.ai.document.Document;
import org.springframework.ai.evaluation.EvaluationRequest;

public class RagasEvaluationRequest extends EvaluationRequest {

    private final String groundTruthAnswer;
    private final String answer;

    public RagasEvaluationRequest(String userText, List<Document> data, String response, String groundTruthAnswer) {
        super(userText, data, response);
        this.answer = response;
        this.groundTruthAnswer = groundTruthAnswer;
    }

    public String getGroundTruthAnswer() {
        return groundTruthAnswer;
    }

    public String getAnswer() {
        return answer;
    }
}
