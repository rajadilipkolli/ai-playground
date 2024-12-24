package com.example.chatbot;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

import com.example.chatbot.common.ContainerConfig;
import com.example.chatbot.model.request.AIChatRequest;
import com.example.chatbot.model.response.AIChatResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.io.IOException;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(
        classes = {ContainerConfig.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ChatbotOllamaApplicationTests {

    @Autowired
    private ObjectMapper objectMapper;

    @LocalServerPort
    private int localServerPort;

    @BeforeAll
    void setUp() {
        RestAssured.port = localServerPort;
    }

    @Test
    void chat() throws IOException {

        Response response = given().contentType(ContentType.JSON)
                .body(
                        new AIChatRequest(
                                "As a cricketer, how many centuries did Sachin Tendulkar scored adding up both One Day International (ODI) and Test centuries ?"))
                .when()
                .post("/api/ai/chat/{conversationId}", "junit1")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .contentType(ContentType.JSON)
                .body("answer", containsString("100"))
                .log()
                .all(true)
                .extract()
                .response();

        AIChatResponse aiChatResponse = objectMapper.readValue(response.asByteArray(), AIChatResponse.class);

        given().contentType(ContentType.JSON)
                .body(new AIChatRequest("Who scored 100 centuries ?"))
                .when()
                .post("/api/ai/chat/{conversationId}", aiChatResponse.conversationId())
                .then()
                .statusCode(HttpStatus.SC_OK)
                .contentType(ContentType.JSON)
                .body("answer", containsString("Sachin"))
                .log()
                .all(true);
    }
}
