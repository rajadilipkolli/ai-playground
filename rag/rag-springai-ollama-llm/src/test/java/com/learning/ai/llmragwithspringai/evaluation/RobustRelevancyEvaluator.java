package com.learning.ai.llmragwithspringai.evaluation;

import java.util.Collections;
import java.util.Map;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.evaluation.EvaluationRequest;
import org.springframework.ai.evaluation.EvaluationResponse;
import org.springframework.ai.evaluation.Evaluator;
import org.springframework.util.Assert;

public class RobustRelevancyEvaluator implements Evaluator {

    private static final PromptTemplate DEFAULT_PROMPT_TEMPLATE = new PromptTemplate("""
                Your task is to evaluate if the response for the query
                is in line with the context information provided.

                You have two options to answer. Either YES or NO.

                Answer YES, if the response for the query
                is in line with context information otherwise NO.

                Query:
                {query}

                Response:
                {response}

                Context:
                {context}

                Answer:
            """);

    private final ChatClient.Builder chatClientBuilder;
    private final PromptTemplate promptTemplate;

    public RobustRelevancyEvaluator(ChatClient.Builder chatClientBuilder) {
        Assert.notNull(chatClientBuilder, "chatClientBuilder cannot be null");
        this.chatClientBuilder = chatClientBuilder;
        this.promptTemplate = DEFAULT_PROMPT_TEMPLATE;
    }

    @Override
    public EvaluationResponse evaluate(EvaluationRequest evaluationRequest) {
        var response = evaluationRequest.getResponseContent();

        // Use getDataList() directly instead of doGetSupportingData() since we are outside the package
        var context = evaluationRequest.getDataList();

        var userMessage = this.promptTemplate.render(
                Map.of("query", evaluationRequest.getUserText(), "response", response, "context", context));

        String evaluationResponse =
                this.chatClientBuilder.build().prompt().user(userMessage).call().content();

        boolean passing = false;
        float score = 0;

        // ROBUST CHECK: ignore case, trim whitespace, and check for contains instead of strict equals
        if (evaluationResponse != null
                && evaluationResponse.strip().toLowerCase().contains("yes")) {
            passing = true;
            score = 1;
        }

        return new EvaluationResponse(passing, score, evaluationResponse, Collections.emptyMap());
    }
}
