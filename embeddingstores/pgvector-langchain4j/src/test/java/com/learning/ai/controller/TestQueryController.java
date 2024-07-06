package com.learning.ai.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.learning.ai.config.AbstractIntegrationTest;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

class TestQueryController extends AbstractIntegrationTest {

    @Test
    void queryEmbeddedStore() throws Exception {
        mockMvc.perform(get("/api/ai/query")
                        .param("question", "What is your favourite sport")
                        .param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer", Matchers.is("I like football.")));
    }

    @Test
    void queryEmbeddedStoreWithMetadata() throws Exception {
        mockMvc.perform(get("/api/ai/query")
                        .param("question", "What is your favourite sport")
                        .param("userId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer", Matchers.is("I like cricket.")));
    }

    @Test
    void queryEmbeddedStoreWithOutMetadata() throws Exception {
        mockMvc.perform(get("/api/ai/query").param("question", "How is weather today"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer", Matchers.is("The weather is good today.")));
    }
}
