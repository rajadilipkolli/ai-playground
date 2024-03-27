package com.example.ai.controller;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.containsStringIgnoringCase;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ChatControllerTest {

    @LocalServerPort
    private int localServerPort;

    @BeforeAll
    void setUp() {
        RestAssured.port = localServerPort;
    }

    @Test
    void testChat() {
        given().param("question", "Hello?")
                .when()
                .get("/api/ai/chat")
                .then()
                .statusCode(200)
                .body("question", containsStringIgnoringCase("Hello?"))
                .body("answer", containsString("Hello!"));
    }

    @Test
    void chatWithPrompt() {
        given().param("subject", "java")
                .when()
                .get("/api/ai/chat-with-prompt")
                .then()
                .statusCode(200)
                .body("answer", containsString("Java"));
    }

    @Test
    void chatWithSystemPrompt() {
        given().param("subject", "cricket")
                .when()
                .get("/api/ai/chat-with-system-prompt")
                .then()
                .statusCode(200)
                .body("answer", containsString("cricket"));
    }
}
