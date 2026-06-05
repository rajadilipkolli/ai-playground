package com.learning.ai.controller;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.hamcrest.Matchers.equalTo;

import com.learning.ai.common.ContainerConfig;
import com.learning.ai.model.request.AIChatRequest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = ContainerConfig.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class QueryControllerTest {

    @LocalServerPort
    private int localServerPort;

    @BeforeAll
    void setUp() {
        RestAssured.port = localServerPort;
    }

    @Test
    void queryEmbeddedStore() {
        given().contentType(ContentType.JSON)
                .body(new AIChatRequest("What is your favourite sport?"))
                .when()
                .post("/api/ai/query")
                .then()
                .statusCode(200)
                .body("answer", equalTo("I like football."));
    }

    @Test
    void queryEmbeddedStoreWithOutOfDomainQuery() {
        given().contentType(ContentType.JSON)
                .body(new AIChatRequest("What is the capital of France?"))
                .when()
                .post("/api/ai/query")
                .then()
                .statusCode(200)
                // RAG should either say it doesn't know, or provide a generic response depending on fallback
                .body(
                        "answer",
                        anyOf(
                                containsStringIgnoringCase("don't know"),
                                containsStringIgnoringCase("not sure"),
                                containsStringIgnoringCase("cannot help"),
                                containsStringIgnoringCase("do not have")));
    }
}
