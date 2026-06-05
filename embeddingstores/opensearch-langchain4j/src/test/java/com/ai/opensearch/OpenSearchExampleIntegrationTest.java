package com.ai.opensearch;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.opensearch.testcontainers.OpenSearchContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = ContainersConfig.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OpenSearchExampleIntegrationTest {

    @Autowired
    private OpenSearchContainer<?> openSearchContainer;

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;

    @BeforeEach
    public void setUpStreams() {
        System.setOut(new PrintStream(outContent));
    }

    @AfterEach
    public void restoreStreams() {
        System.setOut(originalOut);
    }

    @Test
    void testOpenSearchExample() throws Exception {
        // Start the container explicitly if not already started by testcontainers extension
        if (!openSearchContainer.isRunning()) {
            openSearchContainer.start();
        }

        String serverUrl = "http://" + openSearchContainer.getHost() + ":" + openSearchContainer.getMappedPort(9200);

        // Run the example with the dynamic URL and empty args (triggering data load)
        OpenSearchExample.run(serverUrl, new String[]{});

        String output = outContent.toString();

        assertThat(output).contains("Cricket is my favourite sport.");
        assertThat(output).contains("\"restaurant_id\": \"40371727\"");
    }
}
