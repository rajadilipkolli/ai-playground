package com.example.ai.controller;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import com.example.ai.model.request.AIChatRequest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.util.Arrays;
import java.util.stream.Stream;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ChatControllerTest {

    private static final int OPENAI_EMBEDDING_DIMENSION = 1536;

    @LocalServerPort
    private int localServerPort;

    @BeforeAll
    void setUp() {
        RestAssured.port = localServerPort;
    }

    @Test
    void testChat() {
        given().contentType(ContentType.JSON)
                .body(defaultChatRequest("Hello?"))
                .when()
                .post("/api/ai/chat")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .contentType(ContentType.JSON)
                .body("answer", containsString("Hello!"));
    }

    @Test
    void shouldReturnBadRequestForMalformedChatRequest() {
        given().contentType(ContentType.JSON)
                .body("{}") // Empty or malformed request body
                .when()
                .post("/api/ai/chat")
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST);
    }

    @ParameterizedTest
    @MethodSource("chatPrompts")
    void shouldChatWithMultiplePrompts(String prompt) {
        given().contentType(ContentType.JSON)
                .body(defaultChatRequest(prompt))
                .when()
                .post("/api/ai/chat-with-prompt")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .contentType(ContentType.JSON)
                .body("answer", containsStringIgnoringCase(prompt));
    }

    @Test
    void chatWithSystemPrompt() {
        given().contentType(ContentType.JSON)
                .body(defaultChatRequest("cricket"))
                .when()
                .post("/api/ai/chat-with-system-prompt")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .contentType(ContentType.JSON)
                .body("answer", containsString("cricket"));
    }

    @Test
    void shouldHandleErrorGracefullyForSystemPrompt() {
        given().contentType(ContentType.JSON)
                .body(defaultChatRequest(""))
                .when()
                .post("/api/ai/chat-with-system-prompt")
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST);
    }

    @Test
    void shouldAnalyzeSentimentAsSarcastic() {
        given().contentType(ContentType.JSON)
                .body(defaultChatRequest("Why did the Python programmer go broke? Because he couldn't C#"))
                .when()
                .post("/api/ai/sentiment/analyze")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .contentType(ContentType.JSON)
                .body("answer", is("SARCASTIC"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"This is a test sentence.", "Another different sentence.", "A third unique test case."})
    void shouldGenerateValidEmbeddingsWithinExpectedRange(String input) {
        String response = given().contentType(ContentType.JSON)
                .body(defaultChatRequest(input))
                .when()
                .post("/api/ai/embedding-client-conversion")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .contentType(ContentType.JSON)
                .extract()
                .jsonPath()
                .get("answer");

        assertThat(response).isNotNull().startsWith("[").endsWith("]");

        double[] doubles = Arrays.stream(response.replaceAll("[\\[\\]]", "").split(","))
                .mapToDouble(Double::parseDouble)
                .toArray();

        assertThat(doubles.length)
                .isEqualTo(OPENAI_EMBEDDING_DIMENSION)
                .as("Dimensions for openai model is %d", OPENAI_EMBEDDING_DIMENSION);

        assertThat(Arrays.stream(doubles).allMatch(value -> value >= -1.0 && value <= 1.0))
                .isTrue()
                .as("All embedding values should be between -1.0 and 1.0");
    }

    @Test
    void shouldHandleErrorCasesGracefully() {
        given().contentType(ContentType.JSON)
                .body(defaultChatRequest(""))
                .when()
                .post("/api/ai/embedding-client-conversion")
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST);
    }

    @Test
    void outputParserWithParam() {
        given().param("actor", "BalaKrishna")
                .when()
                .get("/api/ai/output")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .contentType(ContentType.JSON)
                .body("actor", containsStringIgnoringCase("Balakrishna"))
                .body("movies", hasSize(greaterThanOrEqualTo(10)));
    }

    @Test
    void outputParserDefaultParam() {
        given().when()
                .get("/api/ai/output")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .contentType(ContentType.JSON)
                .body("actor", is("Jr NTR"))
                .body("movies", hasSize(greaterThanOrEqualTo(11)));
    }

    @Test
    void testRagWithSimpleStoreProvidesValidResponse() {
        given().contentType(ContentType.JSON)
                .body(defaultChatRequest(
                        "Which is the restaurant with the highest grade that has a cuisine as American ?"))
                .when()
                .post("/api/ai/rag")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .contentType(ContentType.JSON)
                .body("answer", containsString("Regina Caterers"));
    }

    static Stream<String> chatPrompts() {
        return Stream.of("java", "spring boot", "ai");
    }

    private AIChatRequest defaultChatRequest(String message) {
        return new AIChatRequest(message);
    }
}
