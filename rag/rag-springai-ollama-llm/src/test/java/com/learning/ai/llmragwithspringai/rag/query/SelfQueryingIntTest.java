package com.learning.ai.llmragwithspringai.rag.query;

import static org.assertj.core.api.Assertions.assertThat;

import com.learning.ai.llmragwithspringai.config.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class SelfQueryingIntTest extends AbstractIntegrationTest {

    @Test
    void shouldAnalyzeQueryUsingOllama() {
        String query = "Show me all HR policies from 2023";
        QueryAnalysisResult result = queryAnalyzer.analyze(query);

        assertThat(result).isNotNull();
        assertThat(result.cleanedQuery()).isNotBlank();
        assertThat(result.cleanedQuery()).doesNotContain("2023");

        assertThat(result.filters()).isNotNull().isNotEmpty().hasSize(2);
    }
}
