package com.learning.ai.llmragwithspringai.evaluation;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * Utility class to load the golden dataset from a JSON resource file.
 * Handles deserialization using Jackson's ObjectMapper.
 */
public class GoldenDatasetLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(GoldenDatasetLoader.class);
    private static final String GOLDEN_DATASET_PATH = "keyword-golden-dataset.json";

    private final JsonMapper jsonMapper;

    public GoldenDatasetLoader(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public GoldenDatasetLoader() {
        this(new JsonMapper());
    }

    /**
     * Load the golden dataset from classpath resources.
     *
     * @return List of GoldenDatasetEntry objects
     * @throws IOException if the resource cannot be read
     */
    public static List<GoldenDatasetEntry> loadGoldenDataset() throws IOException {
        List<GoldenDatasetEntry> entries = new GoldenDatasetLoader().loadDataset(GOLDEN_DATASET_PATH);
        LOGGER.info("Loaded {} golden dataset entries from {}", entries.size(), GOLDEN_DATASET_PATH);
        return entries;
    }

    public List<GoldenDatasetEntry> loadDataset(String classpathResource) throws IOException {
        try (InputStream is = new ClassPathResource(classpathResource).getInputStream()) {
            return jsonMapper.readValue(is, new TypeReference<List<GoldenDatasetEntry>>() {});
        }
    }
}
