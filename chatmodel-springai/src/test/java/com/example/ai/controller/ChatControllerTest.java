package com.example.ai.controller;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import io.restassured.RestAssured;

public class ChatControllerTest {

    @Test
    void testChat() {
        RestAssured.given()
            .param("question", "Hello?")
        .when()
            .get("http://localhost:8080/api/ai/chat")
        .then()
            .statusCode(200)
            .body("question", Matchers.equalTo("Hello?"))
            .body("answer", Matchers.equalTo("Hi!"));
    }
}
