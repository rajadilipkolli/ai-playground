package com.example.ai.controller;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

import com.example.ai.model.request.AIChatRequest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
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
        given().contentType(ContentType.JSON)
                .body(new AIChatRequest("Hello?"))
                .when()
                .post("/api/ai/chat")
                .then()
                .statusCode(200)
                .body("answer", containsString("Hello!"));
    }

    @Test
    void chatWithPrompt() {
        given().contentType(ContentType.JSON)
                .body(new AIChatRequest("java"))
                .when()
                .post("/api/ai/chat-with-prompt")
                .then()
                .statusCode(200)
                .body("answer", containsString("Java"));
    }

    @Test
    void chatWithSystemPrompt() {
        given().contentType(ContentType.JSON)
                .body(new AIChatRequest("cricket"))
                .when()
                .post("/api/ai/chat-with-system-prompt")
                .then()
                .statusCode(200)
                .body("answer", containsString("cricket"));
    }
}
