package com.learning.ai.llmragwithspringai.evaluation;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.json.JsonMapper;

/**
 * Utility class to load the golden dataset from a JSON resource file.
 * Handles deserialization using Jackson's ObjectMapper.
 */
public class GoldenDatasetLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(GoldenDatasetLoader.class);
    private static final String GOLDEN_DATASET_PATH = "golden-dataset.json";

    private static final JsonMapper objectMapper = new JsonMapper();

    /**
     * Load the golden dataset from classpath resources.
     *
     * @return List of GoldenDatasetEntry objects
     * @throws IOException if the resource cannot be read
     */
    public static List<GoldenDatasetEntry> loadGoldenDataset() throws IOException {
        try (InputStream inputStream =
                GoldenDatasetLoader.class.getClassLoader().getResourceAsStream(GOLDEN_DATASET_PATH)) {
            if (inputStream == null) {
                throw new IOException("Golden dataset file not found on classpath: " + GOLDEN_DATASET_PATH);
            }
            GoldenDatasetEntry[] entries = objectMapper.readValue(inputStream, GoldenDatasetEntry[].class);
            LOGGER.info("Loaded {} golden dataset entries from {}", entries.length, GOLDEN_DATASET_PATH);
            return Arrays.asList(entries);
        }
    }
}
