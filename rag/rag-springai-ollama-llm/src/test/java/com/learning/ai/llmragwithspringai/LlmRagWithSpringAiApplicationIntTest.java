package com.learning.ai.llmragwithspringai;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import com.learning.ai.llmragwithspringai.config.AbstractIntegrationTest;
import com.learning.ai.llmragwithspringai.model.request.AIChatRequest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

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
    void uploadPdfContent() throws IOException, URISyntaxException {
        given().multiPart("file", getPath("/Rohit_Gurunath_Sharma.pdf").toFile())
                .when()
                .post("/api/data/v1/upload")
                .then()
                .statusCode(200)
                .log()
                .all();
    }

    @Test
    @Order(2)
    void uploadPdfContentCount() {
        given().when()
                .get("/api/data/v1/count")
                .then()
                .statusCode(200)
                .body("count", is(1))
                .log()
                .all();
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
                .body("queryResponse", containsStringIgnoringCase("yes"))
                .log()
                .all();
    }

    @Test
    @Order(102)
    void testRag2() {
        given().contentType(ContentType.JSON)
                .body(new AIChatRequest("Did Rohit Sharma won ICC Mens T20 World Cup 2016 ?"))
                .when()
                .post("/api/ai/chat")
                .then()
                .statusCode(200)
                .body("queryResponse", containsStringIgnoringCase("No"))
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
                .header(HttpHeaders.CONTENT_TYPE, is(MediaType.APPLICATION_PROBLEM_JSON_VALUE))
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
                .header(HttpHeaders.CONTENT_TYPE, is(MediaType.APPLICATION_PROBLEM_JSON_VALUE))
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
                .header(HttpHeaders.CONTENT_TYPE, is(MediaType.APPLICATION_PROBLEM_JSON_VALUE))
                .body("detail", is("Invalid request content."))
                .body("instance", is("/api/ai/chat"))
                .body("title", is("Constraint Violation"))
                .body("violations", hasSize(1))
                .body("violations[0].field", is("question"))
                .body("violations[0].message", containsString("Invalid characters in query"))
                .log();
    }

    @Test
    @Order(114)
    void testNullRequestBody() {
        given().contentType(ContentType.JSON)
                .body(Optional.empty())
                .when()
                .post("/api/ai/chat")
                .then()
                .statusCode(400)
                .header(HttpHeaders.CONTENT_TYPE, is(MediaType.APPLICATION_PROBLEM_JSON_VALUE))
                .body("detail", is("Failed to read request"))
                .body("instance", is("/api/ai/chat"))
                .body("title", is("Bad Request"))
                .log();
    }

    @Test
    @Order(115)
    void testUnsupportedContentType() {
        given().contentType("text/plain")
                .body("Is Rohit Sharma a batsman?")
                .when()
                .post("/api/ai/chat")
                .then()
                .statusCode(415)
                .header(HttpHeaders.CONTENT_TYPE, is(MediaType.APPLICATION_PROBLEM_JSON_VALUE))
                .body("detail", is("Content-Type 'text/plain;charset=ISO-8859-1' is not supported."))
                .body("instance", is("/api/ai/chat"))
                .body("title", is("Unsupported Media Type"))
                .log();
    }

    @Test
    @Order(116)
    void testMissingQuestionField() {
        given().contentType(ContentType.JSON)
                .body("{}")
                .when()
                .post("/api/ai/chat")
                .then()
                .statusCode(400)
                .header(HttpHeaders.CONTENT_TYPE, is(MediaType.APPLICATION_PROBLEM_JSON_VALUE))
                .body("detail", is("Invalid request content."))
                .body("instance", is("/api/ai/chat"))
                .body("title", is("Constraint Violation"))
                .body("violations", hasSize(1))
                .body("violations[0].field", is("question"))
                .body("violations[0].message", containsString("Query cannot be empty"))
                .log();
    }

    @Test
    @Order(117)
    void testInvalidJsonStructure() {
        given().contentType(ContentType.JSON)
                .body("{invalid json}")
                .when()
                .post("/api/ai/chat")
                .then()
                .statusCode(400)
                .header(HttpHeaders.CONTENT_TYPE, is(MediaType.APPLICATION_PROBLEM_JSON_VALUE))
                .body("detail", is("Failed to read request"))
                .body("instance", is("/api/ai/chat"))
                .body("title", is("Bad Request"))
                .log();
    }

    private Path getPath(String fileName) throws URISyntaxException, IOException {
        return Paths.get(new ClassPathResource(fileName).getURL().toURI());
    }
}
