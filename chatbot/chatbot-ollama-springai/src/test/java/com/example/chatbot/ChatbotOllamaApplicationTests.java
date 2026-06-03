package com.example.chatbot;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.hamcrest.Matchers.emptyOrNullString;
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
                .body("answer", not(emptyOrNullString()))
                .log()
                .all(true)
                .extract()
                .response();

        AIChatResponse aiChatResponse = jsonMapper.readValue(response.asByteArray(), AIChatResponse.class);

        given().contentType(ContentType.JSON)
                .body(new AIChatRequest("Who scored 100 centuries ?"))
                .when()
                .post("/api/ai/chat/{conversationId}", aiChatResponse.conversationId())
                .then()
                .statusCode(HttpStatus.SC_OK)
                .contentType(ContentType.JSON)
                .body("answer", containsStringIgnoringCase("Sachin"))
                .log()
                .all(true);
    }
}
