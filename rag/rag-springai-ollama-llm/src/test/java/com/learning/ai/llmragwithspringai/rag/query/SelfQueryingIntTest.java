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
        // Verify cleaned query is different from original or filters were extracted
        boolean queryWasCleaned = !result.cleanedQuery().equals(query);
        boolean filtersWereExtracted = !result.filters().isEmpty();
        assertThat(queryWasCleaned || filtersWereExtracted)
                .as("QueryAnalyzer should either clean the query or extract filters")
                .isTrue();

        assertThat(result.filters()).isNotNull().isNotEmpty();
        // Optionally verify expected filter keys are present
        assertThat(result.filters()).containsKeys("category", "year");
    }
}
