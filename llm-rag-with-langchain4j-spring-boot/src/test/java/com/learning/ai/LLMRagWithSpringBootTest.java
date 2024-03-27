package com.learning.ai;

import static io.restassured.RestAssured.when;

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
}
