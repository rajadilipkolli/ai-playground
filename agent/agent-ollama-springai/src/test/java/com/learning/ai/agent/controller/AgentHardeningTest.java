package com.learning.ai.agent.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.learning.ai.agent.service.AgentService;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.ProblemDetail;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(controllers = AgentController.class)
@AutoConfigureMockMvc
class AgentHardeningTest {

    // Use MockitoBean to mock the service layer to simulate AI failures
    @MockitoBean
    private AgentService agentService;

    @Autowired
    private MockMvcTester mockMvcTester;

    @Test
    void testNonTransientAiExceptionReturns500() {
        when(agentService.chat(any(), any())).thenThrow(new NonTransientAiException("Simulated API Key failure"));

        mockMvcTester
                .post()
                .uri("/api/agent/error-session")
                .contentType(ContentType.JSON.toString())
                .content("""
                    {"message": "Hello"}
                    """)
                .exchange()
                .assertThat()
                .hasStatus(500)
                .bodyJson()
                .convertTo(ProblemDetail.class)
                .satisfies(problemDetail -> {
                    assertThat(problemDetail.getTitle()).contains("AI Service Error");
                    assertThat(problemDetail.getDetail()).contains("Simulated API Key failure");
                });
    }

    @Test
    void testGenericExceptionReturns500() {
        when(agentService.chat(any(), any())).thenThrow(new RuntimeException("Unknown runtime issue"));

        mockMvcTester
                .post()
                .uri("/api/agent/error-session")
                .contentType(ContentType.JSON.toString())
                .content("""
                    {"message": "Hello"}
                    """)
                .exchange()
                .assertThat()
                .hasStatus(500)
                .bodyJson()
                .convertTo(ProblemDetail.class)
                .satisfies(problemDetail -> {
                    assertThat(problemDetail.getTitle()).contains("Internal Server Error");
                    assertThat(problemDetail.getDetail()).contains("Unknown runtime issue");
                });
    }
}
