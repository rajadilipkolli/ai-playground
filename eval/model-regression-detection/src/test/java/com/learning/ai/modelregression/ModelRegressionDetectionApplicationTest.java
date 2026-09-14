package com.learning.ai.modelregression;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.learning.ai.modelregression.config.AbstractIntegrationTest;
import com.learning.ai.modelregression.model.EmailRequest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(value = MethodOrderer.OrderAnnotation.class)
class ModelRegressionDetectionApplicationTest extends AbstractIntegrationTest {

    @LocalServerPort
    private int localServerPort;

    @BeforeAll
    void setUp() {
        RestAssured.port = localServerPort;
    }

    @Test
    @Order(1)
    void contextLoadsAndDatabaseInitialized() {
        // Assert that the runs table exists by counting it (will return 0, but no SQL exception)
        Integer count = jdbcTemplate.queryForObject("SELECT count(*) FROM runs", Integer.class);
        assertTrue(count >= 0);
    }

    @Test
    @Order(2)
    void testEmailClassificationEndpoint() {
        // Note: this test requires the ollama model to be running and pulled,
        // which testcontainers will handle if configured correctly, but it might fail
        // if the model "llama3.2" isn't available in the testcontainer.
        // For standard int tests we can still hit the endpoint and verify standard parsing/fallbacks.
        // If Ollama fails, we should receive the fallback response.

        io.restassured.response.Response response = given().contentType(ContentType.JSON)
                .body(new EmailRequest("I want to cancel my subscription and get a refund."))
                .when()
                .post("/api/v1/classifier");

        if (response.statusCode() == 200) {
            response.then()
                    .body("category", notNullValue())
                    .body("summary", notNullValue())
                    .log()
                    .all();

            String category = response.jsonPath().getString("category");
            assertTrue(
                    category.toLowerCase().contains("refund")
                            || category.toLowerCase().contains("cancel")
                            || category.toLowerCase().contains("billing")
                            || category.toLowerCase().contains("account"),
                    "Category should reflect cancellation/refund request");
        } else {
            response.then().statusCode(503);
        }
    }

    @Test
    @Order(3)
    void testEmailClassificationEndpoint_ValidationFailure() {
        given().contentType(ContentType.JSON)
                .body("{}") // Missing emailText
                .when()
                .post("/api/v1/classifier")
                .then()
                .statusCode(400)
                .header(HttpHeaders.CONTENT_TYPE, is(MediaType.APPLICATION_PROBLEM_JSON_VALUE))
                .body("title", is("Constraint Violation"))
                .body("violations[0].field", is("emailText"))
                .log()
                .all();
    }

    @Test
    @Order(4)
    void testActuatorHealth() {
        given().when().get("/actuator/health").then().statusCode(200).body("status", is("UP"));
    }
}
