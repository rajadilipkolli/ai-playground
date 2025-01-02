package com.example.ai.controller;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import com.example.ai.model.request.AIChatRequest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.util.Arrays;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
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
    void sentimentAnalyzer() {
        given().contentType(ContentType.JSON)
                .body(new AIChatRequest("Why did the Python programmer go broke? Because he couldn't C#"))
                .when()
                .post("/api/ai/sentiment/analyze")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .contentType(ContentType.JSON)
                .body("answer", is("SARCASTIC"));
    }

    @Test
    void embeddingClientConversion() {
        String response = given().contentType(ContentType.JSON)
                .body(new AIChatRequest("This is a test sentence for embedding conversion."))
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
    void outputParser() {
        given().param("actor", "Jr NTR")
                .when()
                .get("/api/ai/output")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .contentType(ContentType.JSON)
                .body("actor", is("Jr NTR"))
                .body("movies", hasSize(greaterThanOrEqualTo(11)));
    }

    @Test
    void ragWithSimpleStore() {
        given().contentType(ContentType.JSON)
                .body(new AIChatRequest(
                        "Which is the restaurant with the highest grade that has a cuisine as American ?"))
                .when()
                .post("/api/ai/rag")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .contentType(ContentType.JSON)
                .body("answer", containsString("Regina Caterers"));
    }
}
