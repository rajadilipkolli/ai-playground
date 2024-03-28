package com.learning.ai.llmragwithspringai;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

import com.learning.ai.llmragwithspringai.config.AbstractIntegrationTest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.web.server.LocalServerPort;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LlmRagWithSpringAiApplicationIntTest extends AbstractIntegrationTest {

    @LocalServerPort
    private int localServerPort;

    @BeforeAll
    void setUp() {
        RestAssured.port = localServerPort;
    }

    @Test
    void testRag() {
        given().param("question", "What trophies did Rohit won")
                .when()
                .get("/api/ai/chat")
                .then()
                .statusCode(200)
                .body("response", containsString("2007 T20 World Cup"))
                .body("response", containsString("2013 ICC Champions Trophy"));
    }

    @Test
    void testRag2() {
        given().param("question", "Who is successful IPL captain")
                .when()
                .get("/api/ai/chat")
                .then()
                .statusCode(200)
                .body("response", containsString("Rohit Sharma"));
    }

    @Test
    void testEmptyQuery() {
        given().param("question", "")
                .when()
                .get("/api/ai/chat")
                .then()
                .statusCode(400)
                .header("Content-Type", is("application/problem+json"))
                .body("detail", is("Invalid request content."))
                .body("instance", is("/api/ai/chat"))
                .body("title", is("Constraint Violation"))
                .body("violations", hasSize(1))
                .body("violations[0].field", is("ragService.question"))
                .body("violations[0].message", containsString("Query cannot be empty"));
    }

    @Test
    void testLongQueryString() {
        String longQuery = "a".repeat(1000); // Example of a very long query string
        given().param("question", longQuery)
                .when()
                .get("/api/ai/chat")
                .then()
                .statusCode(400)
                .header("Content-Type", is("application/problem+json"))
                .body("detail", is("Invalid request content."))
                .body("instance", is("/api/ai/chat"))
                .body("title", is("Constraint Violation"))
                .body("violations", hasSize(1))
                .body("violations[0].field", is("ragService.question"))
                .body("violations[0].message", containsString("Query exceeds maximum length"));
    }

    @Test
    void testSpecialCharactersInQuery() {
        given().param("question", "@#$%^&*()")
                .when()
                .get("/api/ai/chat")
                .then()
                .statusCode(400)
                .header("Content-Type", is("application/problem+json"))
                .body("detail", is("Invalid request content."))
                .body("instance", is("/api/ai/chat"))
                .body("title", is("Constraint Violation"))
                .body("violations", hasSize(1))
                .body("violations[0].field", is("ragService.question"))
                .body("violations[0].message", containsString("Invalid characters in query"))
                .log();
    }
}
