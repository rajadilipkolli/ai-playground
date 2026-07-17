package com.learning.ai.agent.controller;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;

import com.learning.ai.agent.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class ActuatorMetricsIT extends AbstractIntegrationTest {

    @Test
    void testActuatorExposesCustomMetrics() {
        // First trigger a call so metrics are created
        given().contentType("application/json")
                .body("{\"message\": \"Hello\"}")
                .when()
                .post("/api/agent/metrics-session")
                .then()
                .statusCode(200);

        // Verify the generic metrics endpoint is up and contains our metrics
        given().when()
                .get("/actuator/metrics")
                .then()
                .statusCode(200)
                .body("names", hasItems("agent.calls", "agent.latency"));

        // Drill down into agent.calls
        given().when()
                .get("/actuator/metrics/agent.calls")
                .then()
                .statusCode(200)
                .body("name", equalTo("agent.calls"));
    }
}
