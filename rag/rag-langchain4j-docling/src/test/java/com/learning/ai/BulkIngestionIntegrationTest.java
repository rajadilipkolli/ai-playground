package com.learning.ai;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import com.learning.ai.config.AbstractIntegrationTest;
import com.learning.ai.model.RetrievalRequest;
import com.learning.ai.model.RetrievalResponse;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.ObjectMapper;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(value = MethodOrderer.OrderAnnotation.class)
class BulkIngestionIntegrationTest extends AbstractIntegrationTest {

    @LocalServerPort
    private int localServerPort;

    private String jobId;

    @BeforeAll
    void setUp() {
        RestAssured.port = localServerPort;
    }

    private Path getPath(String fileName) throws URISyntaxException, IOException {
        return Path.of(new ClassPathResource(fileName).getURL().toURI());
    }

    /** Uploads the sample PDF and captures the asynchronous ingestion job identifier. */
    @Test
    @Order(1)
    void uploadPdfContentAndAssertJobId() throws IOException, URISyntaxException {
        jobId = given().auth()
                .preemptive()
                .basic("admin", "admin123")
                .multiPart("files", getPath("file-sample_150kB.pdf").toFile())
                .when()
                .post("/api/ingest/batch")
                .then()
                .statusCode(202)
                .body(notNullValue())
                .extract()
                .asString();

        assertThat(jobId).isNotBlank();
    }

    @Test
    @Order(2)
    void pollJobStatusUntilCompleted() {
        await().atMost(Duration.ofMinutes(5))
                .pollInterval(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    given().auth()
                            .preemptive()
                            .basic("admin", "admin123")
                            .when()
                            .get("/api/ingest/jobs/{jobId}", jobId)
                            .then()
                            .statusCode(200)
                            .body("status", equalTo("COMPLETED"))
                            .body("failedFiles", equalTo(0))
                            .body("processedFiles", equalTo(1))
                            .body("totalFiles", equalTo(1));
                });
    }

    /** Verifies that the sample PDF produced entries in the vector store. */
    @Test
    @Order(3)
    void verifyVectorStoreRows() {
        Integer rowCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM vector_store WHERE metadata->>'source_filename' = ?",
                Integer.class,
                "file-sample_150kB.pdf");
        assertThat(rowCount).isNotNull().isGreaterThan(0);
    }

    /** Verifies the structural metadata stored for the sample PDF. */
    @Test
    @Order(4)
    void verifyVectorStoreMetadata() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT metadata FROM vector_store WHERE metadata->>'source_filename' = ?", "file-sample_150kB.pdf");

        String firstDocumentId = null;
        boolean foundText = false;
        boolean foundHeadingWithSectionPath = false;

        ObjectMapper mapper = new ObjectMapper();

        for (Map<String, Object> row : rows) {
            Object metaObj = row.get("metadata");
            String metadataStr = metaObj.toString();
            Map<String, Object> metadata = mapper.readValue(metadataStr, Map.class);

            assertThat(metadata.get("source_filename")).isEqualTo("file-sample_150kB.pdf");

            String documentId = (String) metadata.get("document_id");
            assertThat(documentId).isNotNull();

            if (firstDocumentId == null) {
                firstDocumentId = documentId;
            }
            assertThat(documentId).isEqualTo(firstDocumentId);

            String elementType = (String) metadata.get("element_type");
            if ("text".equals(elementType)) foundText = true;
            if ("heading".equals(elementType)
                    && metadata.containsKey("section_path")
                    && metadata.get("section_path") != null) {
                foundHeadingWithSectionPath = true;
            }
        }

        assertThat(firstDocumentId).isNotNull();
        assertThat(foundText).isTrue();
        assertThat(foundHeadingWithSectionPath).isTrue();
    }

    /** Verifies that table content from the sample PDF can be retrieved. */
    @Test
    @Order(5)
    void retrieveTableContent() {
        RetrievalRequest request = new RetrievalRequest(
                "In eleifend velit vitae libero sollicitudin euismod", 1, 0.1, "table", null, null, null);

        RetrievalResponse response = given().contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/retrieve")
                .then()
                .statusCode(200)
                .extract()
                .as(RetrievalResponse.class);

        assertThat(response.matches()).isNotEmpty();

        var match = response.matches().getFirst();
        assertThat(match.text()).containsIgnoringCase("Lorem ipsum");
        assertThat(match.text()).containsIgnoringCase("In eleifend velit vitae libero sollicitudin euismod");

        var metadata = match.metadata();
        assertThat(metadata).containsEntry("element_type", "table");
        assertThat(metadata).containsKey("section_path");
        assertThat(metadata).containsEntry("source_filename", "file-sample_150kB.pdf");
    }
}
