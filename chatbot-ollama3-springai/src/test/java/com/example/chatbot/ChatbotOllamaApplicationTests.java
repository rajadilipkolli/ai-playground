package com.example.chatbot;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

import com.example.chatbot.model.request.AIChatRequest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(
        classes = {TestChatbotOllamaApplication.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ChatbotOllamaApplicationTests {

    @LocalServerPort
    private int localServerPort;

    @BeforeAll
    void setUp() {
        RestAssured.port = localServerPort;
    }

    @Test
    void contextLoads() {
        given().contentType(ContentType.JSON)
                .body(new AIChatRequest("Hello?"))
                .when()
                .post("/api/ai/chat")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .contentType(ContentType.JSON)
                .body("answer", containsString("help you"));
    }
}
