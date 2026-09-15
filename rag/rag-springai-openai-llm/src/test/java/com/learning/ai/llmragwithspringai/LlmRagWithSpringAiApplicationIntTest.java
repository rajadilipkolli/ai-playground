package com.learning.ai.llmragwithspringai;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

import com.learning.ai.llmragwithspringai.config.AbstractIntegrationTest;
import com.learning.ai.llmragwithspringai.model.request.AIChatRequest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;

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
        given().contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(new AIChatRequest("What trophies did Rohit won?"))
                .when()
                .post("/api/ai/chat")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(
                        "response",
                        allOf(
                                not(emptyOrNullString()),
                                containsStringIgnoringCase("T20"),
                                anyOf(containsStringIgnoringCase("Champions"), containsStringIgnoringCase("IPL"))))
                .log()
                .all();
    }

    @Test
    void testRag2() {
        given().contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(new AIChatRequest("Who is successful IPL captain?"))
                .when()
                .post("/api/ai/chat")
                .then()
                .statusCode(200)
                .body("response", allOf(not(emptyOrNullString()), containsStringIgnoringCase("Rohit")))
                .log()
                .all();
    }

    @Test
    void testRagNoMatchingDocuments() {
        given().contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(new AIChatRequest("Who won the FIFA World Cup in 2022?"))
                .when()
                .post("/api/ai/chat")
                .then()
                .statusCode(200)
                .body(
                        "response",
                        matchesRegex(
                                "(?i).*(don'?t know|do not know|not sure|unable to answer|cannot answer|no idea|unsure).*"))
                .log()
                .all();
    }

    @Test
    void testEmptyQuery() {
        given().contentType(ContentType.JSON)
                .body(new AIChatRequest(""))
                .when()
                .post("/api/ai/chat")
                .then()
                .statusCode(400)
                .header("Content-Type", is("application/problem+json"))
                .body("detail", is("Invalid request content."))
                .body("instance", is("/api/ai/chat"))
                .body("title", is("Constraint Violation"))
                .body("violations", hasSize(1))
                .body("violations[0].field", is("question"))
                .body("violations[0].message", containsString("Query cannot be empty"))
                .log()
                .all();
    }

    @Test
    void testLongQueryString() {
        String longQuery = "a".repeat(1000); // Example of a very long query string
        given().contentType(ContentType.JSON)
                .body(new AIChatRequest(longQuery))
                .when()
                .post("/api/ai/chat")
                .then()
                .statusCode(400)
                .header("Content-Type", is("application/problem+json"))
                .body("detail", is("Invalid request content."))
                .body("instance", is("/api/ai/chat"))
                .body("title", is("Constraint Violation"))
                .body("violations", hasSize(1))
                .body("violations[0].field", is("question"))
                .body("violations[0].message", containsString("Query exceeds maximum length"))
                .log()
                .all();
    }

    @Test
    void testSpecialCharactersInQuery() {
        given().contentType(ContentType.JSON)
                .body(new AIChatRequest("@#$%^&*()"))
                .when()
                .post("/api/ai/chat")
                .then()
                .statusCode(400)
                .header("Content-Type", is("application/problem+json"))
                .body("detail", is("Invalid request content."))
                .body("instance", is("/api/ai/chat"))
                .body("title", is("Constraint Violation"))
                .body("violations", hasSize(1))
                .body("violations[0].field", is("question"))
                .body("violations[0].message", containsString("Invalid characters in query"))
                .log()
                .all();
    }
}
