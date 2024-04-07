package com.example.ai.controller;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import com.example.ai.model.request.AIChatRequest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.apache.http.HttpStatus;
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
                .statusCode(HttpStatus.SC_OK)
                .contentType(ContentType.JSON)
                .body("answer", containsString("Hello!"));
    }

    @Test
    void chatWithPrompt() {
        given().contentType(ContentType.JSON)
                .body(new AIChatRequest("java"))
                .when()
                .post("/api/ai/chat-with-prompt")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .contentType(ContentType.JSON)
                .body("answer", containsString("Java"));
    }

    @Test
    void chatWithSystemPrompt() {
        given().contentType(ContentType.JSON)
                .body(new AIChatRequest("cricket"))
                .when()
                .post("/api/ai/chat-with-system-prompt")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .contentType(ContentType.JSON)
                .body("answer", containsString("cricket"));
    }

    @Test
    void outputParser() {
        given().param("actor", "Jr NTR")
                .when()
                .get("/api/ai/output")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .contentType(ContentType.JSON)
                .body("actor", is("Jr NTR"))
                .body("movies", hasSize(greaterThanOrEqualTo(25)));
    }

    @Test
    void ragWithSimpleStore() {
        given().contentType(ContentType.JSON)
                .body(new AIChatRequest("which is the restaurant with highest grade that has cuisine as American ?"))
                .when()
                .post("/api/ai/rag")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .contentType(ContentType.JSON)
                .body("answer", containsString("American cuisine is \"Regina Caterers\""));
    }
}
