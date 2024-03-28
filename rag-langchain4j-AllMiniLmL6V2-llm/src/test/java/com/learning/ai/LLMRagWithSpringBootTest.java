package com.learning.ai;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.notNullValue;

import io.restassured.RestAssured;
import io.restassured.http.Method;
import org.apache.http.HttpStatus;
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

    @BeforeAll
    public void setUp() {
        RestAssured.port = serverPort;
    }

    @Test
    void whenRequestGet_thenOK() {
        when().request(Method.GET, "/api/chat").then().statusCode(HttpStatus.SC_OK);
    }

    @Test
    void whenRequestGetTime_thenOK() {
        given().param("message", "What is the time now?")
                .when()
                .request(Method.GET, "/api/chat")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body("response", notNullValue());
    }
}
