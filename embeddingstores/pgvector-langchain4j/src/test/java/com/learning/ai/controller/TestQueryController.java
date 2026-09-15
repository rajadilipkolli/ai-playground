package com.learning.ai.controller;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.learning.ai.config.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class TestQueryController extends AbstractIntegrationTest {

    @Test
    void queryEmbeddedStore() throws Exception {
        mockMvc.perform(get("/api/ai/query")
                        .param("question", "What is your favourite sport")
                        .param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer", is("I like football.")));
    }

    @Test
    void queryEmbeddedStoreWithMetadata() throws Exception {
        mockMvc.perform(get("/api/ai/query")
                        .param("question", "What is your favourite sport")
                        .param("userId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer", is("I like cricket.")));
    }

    @Test
    void queryEmbeddedStoreWithOutMetadata() throws Exception {
        mockMvc.perform(get("/api/ai/query").param("question", "How is weather today"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer", is("The weather is good today.")));
    }

    @Test
    void queryEmbeddedStoreWithInvalidMetadata() throws Exception {
        mockMvc.perform(get("/api/ai/query")
                        .param("question", "What is your favourite sport")
                        .param("userId", "99"))
                .andExpect(status().isOk())
                // Since userId=99 doesn't match the relevant docs, it shouldn't return football or cricket
                .andExpect(jsonPath("$.answer", not(is("I like football."))))
                .andExpect(jsonPath("$.answer", not(is("I like cricket."))));
    }
}
