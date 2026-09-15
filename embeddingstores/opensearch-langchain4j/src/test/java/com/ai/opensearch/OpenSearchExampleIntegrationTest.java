package com.ai.opensearch;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.opensearch.testcontainers.OpenSearchContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@SpringBootTest(classes = {TestApplication.class, ContainersConfig.class}, properties = "spring.main.allow-bean-definition-overriding=true")
@ExtendWith(OutputCaptureExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OpenSearchExampleIntegrationTest {

    @Autowired
    private OpenSearchContainer<?> openSearchContainer;

    @Test
    void testOpenSearchExample(CapturedOutput output) throws Exception {
        // Start the container explicitly if not already started by testcontainers extension
        if (!openSearchContainer.isRunning()) {
            openSearchContainer.start();
        }

        String serverUrl = "http://" + openSearchContainer.getHost() + ":" + openSearchContainer.getMappedPort(9200);

        // Run the example with the dynamic URL and empty args (triggering data load)
        OpenSearchExample.run(serverUrl, new String[]{});

        String logOutput = output.getOut();

        assertThat(logOutput).contains("Cricket is my favourite sport.");
        assertThat(logOutput).contains("\"restaurant_id\": \"40839319\"");
    }
}
