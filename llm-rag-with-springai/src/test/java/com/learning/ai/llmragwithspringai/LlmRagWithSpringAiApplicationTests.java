package com.learning.ai.llmragwithspringai;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

@SpringBootTest(classes = TestLlmRagWithSpringAiApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LlmRagWithSpringAiApplicationTests {

	@LocalServerPort
	private int localServerPort;

	@BeforeAll
	void setUp() {
		RestAssured.port = localServerPort;
	}

	@Test
	void testRag() {
		given().param("question", "What trophy did Rohit won")
				.when()
				.get("/api/ai/chat")
				.then()
				.statusCode(200)
				.body("response", containsString("2007 T20 World Cup and the 2013 ICC Champions Trophy"));
	}

}
