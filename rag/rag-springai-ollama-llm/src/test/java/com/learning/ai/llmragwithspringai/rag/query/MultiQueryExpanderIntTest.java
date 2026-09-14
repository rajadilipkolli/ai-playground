package com.learning.ai.llmragwithspringai.rag.query;

import static org.assertj.core.api.Assertions.assertThat;

import com.learning.ai.llmragwithspringai.config.AbstractIntegrationTest;
import com.learning.ai.llmragwithspringai.config.properties.RagQueryProperties;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.rag.Query;

class MultiQueryExpanderIntTest extends AbstractIntegrationTest {

    @Test
    void shouldGenerateQueryVariationsThroughOllama() {
        RagQueryProperties properties = new RagQueryProperties();
        properties.getMultiquery().setEnabled(true);
        properties.getMultiquery().setVariations(2);

        MultiQueryExpander transformer = new MultiQueryExpander(chatClientBuilder, properties);

        Query originalQuery = new Query("What is the best way to optimize a Postgres database?");
        List<Query> results = transformer.expand(originalQuery);

        // We expect the original query + at least a few generated ones
        assertThat(results).hasSizeGreaterThan(1);
        assertThat(results.getFirst().text()).isEqualTo("What is the best way to optimize a Postgres database?");

        // Ensure that the LLM generated unique questions
        for (int i = 1; i < results.size(); i++) {
            assertThat(results.get(i).text()).isNotBlank();
            assertThat(results.get(i).text()).isNotEqualTo(originalQuery.text());
        }
    }
}
