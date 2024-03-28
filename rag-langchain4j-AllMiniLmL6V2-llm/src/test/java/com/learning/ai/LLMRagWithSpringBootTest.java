package com.learning.ai;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;

import com.learning.ai.domain.request.AIChatRequest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
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
    void whenRequestPost_thenOK() {
        given().contentType(ContentType.JSON)
                .body(new AIChatRequest(
                        "what should I know about the transition to consumer direct care network washington?"))
                .when()
                .request(Method.POST, "/api/ai/chat")
                .then()
                .statusCode(HttpStatus.SC_OK);
    }

    @Test
    void whenRequestGetTime_thenOK() {
        given().contentType(ContentType.JSON)
                .body(new AIChatRequest("What is the time now?"))
                .when()
                .request(Method.POST, "/api/ai/chat")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body("response", notNullValue());
    }
}
