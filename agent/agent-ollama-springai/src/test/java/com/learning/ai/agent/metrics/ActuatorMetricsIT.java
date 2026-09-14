package com.learning.ai.agent.metrics;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

import com.learning.ai.agent.AbstractIntegrationTest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalServerPort;

class ActuatorMetricsIT extends AbstractIntegrationTest {

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    @Test
    void testMetricsAreExported() {
        // First invoke the agent to ensure metrics are recorded
        agentService.chat("metrics-session", "What is 2 + 2? Use the calculator.");

        // Check Prometheus endpoint
        given().when()
                .get("/actuator/prometheus")
                .then()
                .statusCode(200)
                // Check for custom metric
                .body(containsString("agent_calls_total"));

        // Check standard Actuator metrics endpoint for specific custom metrics
        given().when()
                .get("/actuator/metrics/agent.calls")
                .then()
                .statusCode(200)
                .body("name", equalTo("agent.calls"))
                .body("measurements[0].value", greaterThanOrEqualTo(1.0f));
    }
}
