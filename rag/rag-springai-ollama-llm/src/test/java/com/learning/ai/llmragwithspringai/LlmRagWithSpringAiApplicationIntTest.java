package com.learning.ai.llmragwithspringai;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import com.learning.ai.llmragwithspringai.config.AbstractIntegrationTest;
import com.learning.ai.llmragwithspringai.model.request.AIChatRequest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.io.File;
import java.io.IOException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ClassPathResource;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(value = MethodOrderer.OrderAnnotation.class)
class LlmRagWithSpringAiApplicationIntTest extends AbstractIntegrationTest {

    @LocalServerPort
    private int localServerPort;

    @BeforeAll
    void setUp() {
        RestAssured.port = localServerPort;
    }

    @Test
    @Order(1)
    void uploadPdfContent() throws IOException {
        given().multiPart("file", getFile("/Rohit_Gurunath_Sharma.pdf"))
                .when()
                .post("/api/data/v1/upload")
                .then()
                .statusCode(200);
    }

    private File getFile(String fileName) throws IOException {
        return new ClassPathResource(fileName).getFile();
    }

    @Test
    @Order(101)
    void testRag() {
        given().contentType(ContentType.JSON)
                .body(new AIChatRequest("Is Rohit Sharma batsman?"))
                .when()
                .post("/api/ai/chat")
                .then()
                .statusCode(200)
                .body("queryResponse", containsString("Yes"))
                .log()
                .all();
    }

    @Test
    @Order(111)
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
                .body("violations[0].message", containsString("Query cannot be empty"));
    }

    @Test
    @Order(112)
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
                .body("violations[0].message", containsString("Query exceeds maximum length"));
    }

    @Test
    @Order(113)
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
                .log();
    }
}
