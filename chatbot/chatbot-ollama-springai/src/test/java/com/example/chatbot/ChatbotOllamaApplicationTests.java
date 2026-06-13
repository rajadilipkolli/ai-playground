package com.example.chatbot;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;

import com.example.chatbot.common.ContainerConfig;
import com.example.chatbot.model.request.AIChatRequest;
import com.example.chatbot.model.response.AIChatResponse;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(
        classes = {ContainerConfig.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ChatbotOllamaApplicationTests {

    @Autowired
    private JsonMapper jsonMapper;

    @LocalServerPort
    private int localServerPort;

    @BeforeAll
    void setUp() {
        RestAssured.port = localServerPort;
    }

    @Test
    void chat() {

        Response response = given().contentType(ContentType.JSON)
                .body(
                        new AIChatRequest(
                                "As a cricketer, how many centuries did Sachin Tendulkar scored adding up both One Day International (ODI) and Test centuries ?"))
                .when()
                .post(
                        "/api/ai/chat/{conversationId}",
                        RandomStringUtils.secure().nextAlphabetic(16))
                .then()
                .statusCode(HttpStatus.SC_OK)
                .contentType(ContentType.JSON)
                .body(
                        "answer",
                        allOf(
                                not(emptyOrNullString()),
                                matchesPattern("(?is).*\\d+.*"),
                                anyOf(containsStringIgnoringCase("centuries"), containsStringIgnoringCase("hundred"))))
                .log()
                .all(true)
                .extract()
                .response();

        AIChatResponse aiChatResponse = jsonMapper.readValue(response.asByteArray(), AIChatResponse.class);

        // Test follow-up response uses context from previous turn
        given().contentType(ContentType.JSON)
                .body(new AIChatRequest("Who scored 100 centuries ?"))
                .when()
                .post("/api/ai/chat/{conversationId}", aiChatResponse.conversationId())
                .then()
                .statusCode(HttpStatus.SC_OK)
                .contentType(ContentType.JSON)
                .body("answer", allOf(not(emptyOrNullString()), containsStringIgnoringCase("Sachin")))
                .log()
                .all(true);
    }

    @Test
    void chatConversationIsolation() {
        String conv1 = RandomStringUtils.secure().nextAlphabetic(16);
        String conv2 = RandomStringUtils.secure().nextAlphabetic(16);

        // Tell a fact to conv1
        given().contentType(ContentType.JSON)
                .body(new AIChatRequest("My favorite color is green."))
                .when()
                .post("/api/ai/chat/{conversationId}", conv1)
                .then()
                .statusCode(HttpStatus.SC_OK);

        // Ask conv2 about the fact
        given().contentType(ContentType.JSON)
                .body(new AIChatRequest("What is my favorite color?"))
                .when()
                .post("/api/ai/chat/{conversationId}", conv2)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body("answer", not(containsStringIgnoringCase("green")))
                .log()
                .all(true);
    }

    @Test
    void chatWithMissingConversationId() {
        given().contentType(ContentType.JSON)
                .body(new AIChatRequest("Hello"))
                .when()
                .post("/api/ai/chat/") // Missing ID
                .then()
                .statusCode(HttpStatus.SC_NOT_FOUND);
    }

    @Test
    void whenQueryHasInvalidCharacters_thenReturns400() {
        given().contentType(ContentType.JSON)
                .body(new AIChatRequest("What is this? <script>alert(1)</script>"))
                .when()
                .post(
                        "/api/ai/chat/{conversationId}",
                        RandomStringUtils.secure().nextAlphabetic(16))
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body("title", org.hamcrest.Matchers.equalTo("Bad Request"))
                .body("detail", org.hamcrest.Matchers.containsString("Invalid request content."));
    }

    @Test
    void whenQueryHasSensitiveWords_thenReturnsBlockedMessage() {
        given().contentType(ContentType.JSON)
                .body(new AIChatRequest("Let's talk about politics and who should win."))
                .when()
                .post(
                        "/api/ai/chat/{conversationId}",
                        RandomStringUtils.secure().nextAlphabetic(16))
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(
                        "answer",
                        org.hamcrest.Matchers.containsString("I'm sorry, but I cannot assist with that topic."));
    }
}
