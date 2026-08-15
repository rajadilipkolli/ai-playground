package com.learning.ai.modelregression.eval.scoring;

import java.util.Map;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Component;

@Component
public class SummaryRelevanceJudge {

    private final ChatClient judgeClient;

    public SummaryRelevanceJudge(ChatClient.Builder chatClientBuilder) {
        // Set low temperature for the judge
        this.judgeClient = chatClientBuilder
                .defaultSystem(
                        "You are an impartial judge evaluating the relevance of a customer support email summary. "
                                + "Rate the summary from 1 to 5, where 1 is completely irrelevant or wrong, and 5 is perfectly accurate and concise.")
                .build();
    }

    public JudgeResult evaluate(String emailText, String expectedSummary, String actualSummary) {
        BeanOutputConverter<JudgeResult> outputParser = new BeanOutputConverter<>(JudgeResult.class);

        String promptText = """
                Evaluate the actual summary against the expected summary for the given email.
                Email: {email}
                Expected Summary: {expected}
                Actual Summary: {actual}

                {format}
                """;

        PromptTemplate template = new PromptTemplate(promptText);
        Prompt prompt = template.create(Map.of(
                "email", emailText,
                "expected", expectedSummary,
                "actual", actualSummary,
                "format", outputParser.getFormat()));

        try {
            String response = judgeClient.prompt(prompt).call().content();
            JudgeResult result = outputParser.convert(response);
            if (result == null || result.score() < 1 || result.score() > 5) {
                return new JudgeResult(3, "Fallback due to invalid score"); // Fallback
            }
            return result;
        } catch (Exception e) {
            return new JudgeResult(3, "Fallback due to exception: " + e.getMessage());
        }
    }

    public record JudgeResult(int score, String reasoning) {}
}
