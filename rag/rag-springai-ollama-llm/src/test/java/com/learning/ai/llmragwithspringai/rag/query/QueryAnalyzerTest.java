package com.learning.ai.llmragwithspringai.rag.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class QueryAnalyzerTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ChatClient.Builder chatClientBuilder;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ChatClient chatClient;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private JsonMapper jsonMapper;

    private QueryAnalyzer queryAnalyzer;

    @BeforeEach
    void setUp() {
        when(chatClientBuilder.build()).thenReturn(chatClient);
        queryAnalyzer = new QueryAnalyzer(chatClientBuilder, jsonMapper);
    }

    @Test
    void shouldExtractFiltersAndReturnCleanedQuery() {
        String llmResponse = """
                {"cleanedQuery":"What are the employee benefits?","filters":{"category":"HR","year":"2023"}}
                """;
        when(chatClient.prompt().system(anyString()).user(anyString()).call().content())
                .thenReturn(llmResponse);

        QueryAnalysisResult result = queryAnalyzer.analyze("What are the employee benefits for HR in 2023?");

        assertThat(result).isNotNull();
        assertThat(result.cleanedQuery()).isEqualTo("What are the employee benefits?");
        assertThat(result.filters()).containsEntry("category", "HR").containsEntry("year", "2023");
    }

    @Test
    void shouldReturnEmptyFiltersIfNoneFound() {
        String llmResponse = """
                {"cleanedQuery":"What is Java?","filters":{}}
                """;
        when(chatClient.prompt().system(anyString()).user(anyString()).call().content())
                .thenReturn(llmResponse);

        QueryAnalysisResult result = queryAnalyzer.analyze("What is Java?");

        assertThat(result).isNotNull();
        assertThat(result.cleanedQuery()).isEqualTo("What is Java?");
        assertThat(result.filters()).isEmpty();
    }

    @Test
    void shouldHandleMarkdownFencedResponse() {
        String llmResponse = """
                ```json
                {"cleanedQuery":"What are the employee benefits?","filters":{"category":"HR"}}
                ```
                """;
        when(chatClient.prompt().system(anyString()).user(anyString()).call().content())
                .thenReturn(llmResponse);

        QueryAnalysisResult result = queryAnalyzer.analyze("What are the employee benefits for HR?");

        assertThat(result).isNotNull();
        assertThat(result.cleanedQuery()).isEqualTo("What are the employee benefits?");
        assertThat(result.filters()).containsEntry("category", "HR");
    }

    @Test
    void shouldHandleMalformedJsonViaRegexFallback() {
        // Simulates qwen2.5:0.5b producing broken JSON like: "filters} {"key":"value"}"
        String llmResponse =
                "{\"cleanedQuery\":\"Show me all HR policies from 2023\",\"filters} {\"category\":\"HR\",\"year\":\"2023\"}";
        when(chatClient.prompt().system(anyString()).user(anyString()).call().content())
                .thenReturn(llmResponse);

        QueryAnalysisResult result = queryAnalyzer.analyze("Show me all HR policies from 2023");

        assertThat(result).isNotNull();
        assertThat(result.filters()).containsEntry("category", "HR").containsEntry("year", "2023");
    }

    @Test
    void shouldFallbackToOriginalQueryWhenCleanedQueryMissing() {
        // Simulates a model that returns only filters without cleanedQuery
        String llmResponse = "{\"category\":\"HR\",\"year\":\"2023\"}";
        when(chatClient.prompt().system(anyString()).user(anyString()).call().content())
                .thenReturn(llmResponse);

        String originalQuery = "Show me all HR policies from 2023";
        QueryAnalysisResult result = queryAnalyzer.analyze(originalQuery);

        assertThat(result).isNotNull();
        assertThat(result.cleanedQuery()).isEqualTo(originalQuery);
    }
}
