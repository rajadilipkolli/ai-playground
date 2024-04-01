package com.learning.ai.controller;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import com.learning.ai.TestNeo4jVectorEmbeddingStoreExample;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = TestNeo4jVectorEmbeddingStoreExample.class)
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
        given().param("question", "What is your favourite sport")
                .when()
                .get("/api/ai/query")
                .then()
                .statusCode(200)
                .body("answer", equalTo("I like football."));
    }
}
