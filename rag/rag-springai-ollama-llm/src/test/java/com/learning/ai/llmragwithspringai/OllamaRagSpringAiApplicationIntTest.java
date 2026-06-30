package com.learning.ai.llmragwithspringai;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import com.learning.ai.llmragwithspringai.config.AbstractIntegrationTest;
import com.learning.ai.llmragwithspringai.model.request.AIChatRequest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(value = MethodOrderer.OrderAnnotation.class)
class OllamaRagSpringAiApplicationIntTest extends AbstractIntegrationTest {

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
                .queryParam("documentType", "profile")
                .queryParam("owner", "cricket_board")
                .queryParam("category", "sports")
                .when()
                .post("/api/data/v1/upload")
                .then()
                .statusCode(200)
                .log()
                .all();
    }

    @Test
    @Order(10)
    void testUploadUnsupportedFileType() {
        given().contentType(ContentType.MULTIPART)
                .multiPart("file", "test.docx", "dummy content".getBytes(), "application/octet-stream")
                .when()
                .post("/api/data/v1/upload")
                .then()
                .statusCode(200)
                .body("status", equalTo("unsupported_format"))
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
                .body("count", is(2))
                .log()
                .all();
    }

    @Test
    @Order(3)
    void testClearCache() {
        given().when()
                .delete("/api/data/v1/cache")
                .then()
                .statusCode(200)
                .body("message", is("Cache cleared successfully"))
                .log()
                .all();
    }

    @Test
    @Order(101)
    void testRag() {
        given().contentType(ContentType.JSON)
                .body(new AIChatRequest("Is Rohit Sharma batsman?", "profile", "cricket_board", "sports", null))
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
                .body(new AIChatRequest("Does Rohit Sharma play for Chennai Super Kings?", "profile", null, null, null))
                .when()
                .post("/api/ai/chat")
                .then()
                .statusCode(200)
                .body("queryResponse", containsStringIgnoringCase("No"))
                .log()
                .all();
    }

    @Test
    @Order(103)
    @DisplayName("should fetch from Cache")
    void testRagWithDiagnostics() {
        given().contentType(ContentType.JSON)
                .body(new AIChatRequest("Is Rohit Sharma batsman?", null, null, "sports", null))
                .queryParam("includeDiagnostics", true)
                .when()
                .post("/api/ai/chat")
                .then()
                .statusCode(200)
                .body("queryResponse", containsStringIgnoringCase("yes"))
                .body("diagnostics.size()", greaterThan(0))
                .log()
                .all();
    }

    @Test
    @Order(104)
    void testRagWithMetadataFilter() {
        given().contentType(ContentType.JSON)
                .body(new AIChatRequest("Is Rohit Sharma batsman?", "profile", "cricket_board", "sports", null))
                .when()
                .post("/api/ai/chat")
                .then()
                .statusCode(200)
                .body("queryResponse", containsStringIgnoringCase("yes"))
                .log()
                .all();
    }

    @Test
    @Order(105)
    void testGuardrailsRejectSensitiveQuery() {
        given().contentType(ContentType.JSON)
                .body(new AIChatRequest("What are your views on politics?", null, null, null, null))
                .when()
                .post("/api/ai/chat")
                .then()
                .statusCode(200)
                .body("queryResponse", containsStringIgnoringCase("I'm sorry, but I cannot assist with that topic."))
                .log()
                .all();
    }

    @Test
    @Order(106)
    void testGuardrailsRejectViolence() {
        given().contentType(ContentType.JSON)
                .body(new AIChatRequest("Tell me about violence and fighting", null, null, null, null))
                .when()
                .post("/api/ai/chat")
                .then()
                .statusCode(200)
                .body("queryResponse", containsStringIgnoringCase("I'm sorry, but I cannot assist with that topic."))
                .log()
                .all();
    }

    @Test
    @Order(107)
    void testChatEndpointWithCalculatorTool() {
        given().contentType(ContentType.JSON)
                .body(new AIChatRequest("What is 15 multiplied by 4?", null, null, null, null))
                .when()
                .post("/api/ai/chat")
                .then()
                .statusCode(200)
                .body("queryResponse", containsString("60"))
                .log()
                .all();
    }

    @Test
    @Order(108)
    void testKnowledgeSearchTool() {
        // Relies on the already ingested Rohit Sharma document
        given().contentType(ContentType.JSON)
                .body(new AIChatRequest(
                        "Find information about Rohit Sharma using the knowledge search tool.",
                        "profile",
                        "cricket_board",
                        "sports",
                        null))
                .when()
                .post("/api/ai/chat")
                .then()
                .statusCode(200)
                .body("queryResponse", containsStringIgnoringCase("batsman"))
                .log()
                .all();
    }

    @Test
    @Order(109)
    void testCalculatorToolWithRceAttempt() {
        given().contentType(ContentType.JSON)
                .body(new AIChatRequest(
                        "Calculate T(java.lang.Runtime).getRuntime().exec('calc')", null, null, null, null))
                .when()
                .post("/api/ai/chat")
                .then()
                .statusCode(200)
                // The LLM may decline to evaluate it or exp4j safely returns an error.
                // We check for common LLM refusal/error phrases
                .body(
                        "queryResponse",
                        org.hamcrest.Matchers.anyOf(
                                containsStringIgnoringCase("error"),
                                containsStringIgnoringCase("cannot"),
                                containsStringIgnoringCase("invalid"),
                                containsStringIgnoringCase("sorry"),
                                containsStringIgnoringCase("unable"),
                                containsStringIgnoringCase("mathematical expression"),
                                containsStringIgnoringCase("provide the expression")))
                .log()
                .all();
    }

    @Test
    @Order(111)
    void testEmptyQuery() {
        given().contentType(ContentType.JSON)
                .body(new AIChatRequest("", null, null, null, null))
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
        String longQuery = "a".repeat(1001); // Example of a very long query string
        given().contentType(ContentType.JSON)
                .body(new AIChatRequest(longQuery, null, null, null, null))
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
                .body(new AIChatRequest("@#$%^&*(", null, null, null, null))
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

    @Test
    @Order(151)
    void testActuatorMetricsRagChat() {
        given().when().get("/actuator/metrics/rag.chat").then().statusCode(200).body("name", is("rag.chat"));
    }

    @Test
    @Order(152)
    void testActuatorPrometheusMetrics() {
        given().when()
                .get("/actuator/prometheus")
                .then()
                .statusCode(200)
                .body(containsString("rag_llm_calls_total"))
                .body(containsString("rag_documents_retrieved_total"))
                .body(containsString("rag_chat_seconds"))
                .body(containsString("rag_cache_hits_total"))
                .body(containsString("rag_cache_misses_total"));
    }

    private Path getPath(String fileName) throws URISyntaxException, IOException {
        return Path.of(new ClassPathResource(fileName).getURL().toURI());
    }
}
