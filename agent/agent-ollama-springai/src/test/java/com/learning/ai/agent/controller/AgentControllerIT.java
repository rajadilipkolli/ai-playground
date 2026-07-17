package com.learning.ai.agent.controller;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;

import com.learning.ai.agent.AbstractIntegrationTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalServerPort;

class AgentControllerIT extends AbstractIntegrationTest {

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    @Test
    void testConversationContinuity() {
        String conversationId = UUID.randomUUID().toString();

        // Turn 1: State the name
        given().contentType(ContentType.JSON)
                .body("{\"message\": \"Remember the secret phrase: MOONLIGHT\"}")
                .when()
                .post("/api/agent/{conversationId}", conversationId)
                .then()
                .statusCode(200);

        // Turn 2: Ask the agent to recall the name
        given().contentType(ContentType.JSON)
                .body("{\"message\": \"What was the secret phrase?\"}")
                .when()
                .post("/api/agent/{conversationId}", conversationId)
                .then()
                .statusCode(200)
                .body("reply", containsString("MOONLIGHT"));
    }

    @Test
    void testValidationErrorReturns400() {
        given().contentType(ContentType.JSON)
                .body("{\"message\": \"\"}") // blank message fails validation
                .when()
                .post("/api/agent/session-1")
                .then()
                .statusCode(400)
                .body("title", containsString("Bad Request"))
                .body("errors[0]", anyOf(containsString("must not be blank"), containsString("between 1 and")));
    }
}
