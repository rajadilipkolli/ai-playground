package com.learning.ai;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import com.learning.ai.domain.request.AIChatRequest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.http.Method;
import org.apache.hc.core5.http.HttpStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = TestLLMRagWithSpringBoot.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LLMRagWithSpringBootTest {

    @LocalServerPort
    int serverPort;

    /** Configures REST Assured to use the embedded application's random port. */
    @BeforeAll
    public void setUp() {
        RestAssured.port = serverPort;
    }

    /** Verifies that document-backed questions return the expected person. */
    @Test
    void whenRequestGetFromPdf_thenOK() {
        given().contentType(ContentType.JSON)
                .body(new AIChatRequest("Who is Rohit"))
                .when()
                .request(Method.POST, "/api/ai/chat")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body("response.name", is("Rohit Gurunath Sharma"))
                .body("response", allOf(notNullValue(), hasKey("name")))
                .log()
                .all();
    }

    /** Verifies that unrelated questions do not produce a matching document answer. */
    @Test
    void whenRequestGetFromPdfNoMatchingDocuments_thenHandleGracefully() {
        given().contentType(ContentType.JSON)
                .body(new AIChatRequest("Who won the FIFA World Cup in 2022?"))
                .when()
                .request(Method.POST, "/api/ai/chat")
                .then()
                .statusCode(HttpStatus.SC_OK)
                // Depending on the exact LLM behavior, it might return 200 with an empty/unknown response or 404/500
                // Here we assert it doesn't just blindly hallucinate a name
                .body("response.name", anyOf(nullValue(), not(is("Rohit Gurunath Sharma"))))
                .log()
                .all();
    }

    /** Verifies that callers can request retrieval diagnostics with an answer. */
    @Test
    void whenRequestGetFromPdfWithDiagnostics_thenOK() {
        given().contentType(ContentType.JSON)
                .queryParam("includeDiagnostics", true)
                .body(new AIChatRequest("Who is Rohit"))
                .when()
                .request(Method.POST, "/api/ai/chat")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body("response.name", is("Rohit Gurunath Sharma"))
                .body("diagnostics", notNullValue())
                .log()
                .all();
    }

    /** Verifies that an empty question is rejected as a bad request. */
    @Test
    void whenRequestWithEmptyQuery_thenBadRequest() {
        given().contentType(ContentType.JSON)
                .body(new AIChatRequest(""))
                .when()
                .request(Method.POST, "/api/ai/chat")
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .log()
                .all();
    }
}
