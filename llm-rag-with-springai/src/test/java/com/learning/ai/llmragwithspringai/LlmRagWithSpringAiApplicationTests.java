package com.learning.ai.llmragwithspringai;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(
        classes = TestLlmRagWithSpringAiApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LlmRagWithSpringAiApplicationTests {

    @LocalServerPort
    private int localServerPort;

    @BeforeAll
    void setUp() {
        RestAssured.port = localServerPort;
    }

    @Test
    void testRag() {
        given().param("question", "What trophy did Rohit won")
                .when()
                .get("/api/ai/chat")
                .then()
                .statusCode(200)
                .body("response", containsString("2007 T20 World Cup and the 2013 ICC Champions Trophy"));
    }

    @Test
    void testEmptyQuery() {
        given().param("question", "")
                .when()
                .get("/api/ai/chat")
                .then()
                .statusCode(400)
                .body("error", containsString("Query cannot be empty"));
    }

    @Test
    void testLongQueryString() {
        String longQuery = "a".repeat(1000); // Example of a very long query string
        given().param("question", longQuery)
                .when()
                .get("/api/ai/chat")
                .then()
                .statusCode(400)
                .body("error", containsString("Query exceeds maximum length"));
    }

    @Test
    void testSpecialCharactersInQuery() {
        given().param("question", "@#$%^&*()")
                .when()
                .get("/api/ai/chat")
                .then()
                .statusCode(400)
                .body("error", containsString("Invalid characters in query"));
    }
}
