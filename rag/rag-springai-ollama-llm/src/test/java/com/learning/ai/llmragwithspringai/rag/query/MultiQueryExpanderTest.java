package com.learning.ai.llmragwithspringai.rag.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.learning.ai.llmragwithspringai.config.properties.RagQueryProperties;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.rag.Query;

@ExtendWith(MockitoExtension.class)
class MultiQueryExpanderTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ChatClient.Builder chatClientBuilder;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ChatClient chatClient;

    private RagQueryProperties properties;

    @BeforeEach
    void setUp() {
        properties = new RagQueryProperties();
        properties.getMultiquery().setVariations(2);

        when(chatClientBuilder.build()).thenReturn(chatClient);
    }

    @Test
    void shouldReturnOriginalQueryAndVariations() {
        String mockLlmResponse = "1. What is the current status of Rohit Sharma?\n"
                + "- Tell me about Rohit Sharma's recent activities.\n"
                + "Who is Rohit Sharma exactly?";

        when(chatClient.prompt().user(anyString()).call().content()).thenReturn(mockLlmResponse);

        MultiQueryExpander transformer = new MultiQueryExpander(chatClientBuilder, properties);

        Query originalQuery = new Query("Who is Rohit Sharma?");
        List<Query> results = transformer.expand(originalQuery);

        assertThat(results).hasSize(4);
        assertThat(results.get(0).text()).isEqualTo("Who is Rohit Sharma?");
        assertThat(results.get(1).text()).isEqualTo("What is the current status of Rohit Sharma?");
        assertThat(results.get(2).text()).isEqualTo("Tell me about Rohit Sharma's recent activities.");
        assertThat(results.get(3).text()).isEqualTo("Who is Rohit Sharma exactly?");
    }

    @Test
    void shouldReturnOnlyOriginalQueryWhenLlmResponseIsBlank() {
        when(chatClient.prompt().user(anyString()).call().content()).thenReturn("");

        MultiQueryExpander transformer = new MultiQueryExpander(chatClientBuilder, properties);

        Query originalQuery = new Query("Who is Rohit Sharma?");
        List<Query> results = transformer.expand(originalQuery);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).text()).isEqualTo("Who is Rohit Sharma?");
    }
}
