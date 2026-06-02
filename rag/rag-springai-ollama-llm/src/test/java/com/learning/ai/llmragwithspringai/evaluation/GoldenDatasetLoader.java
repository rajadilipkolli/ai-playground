package com.learning.ai.llmragwithspringai.evaluation;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

public class GoldenDatasetLoader {

    private final ObjectMapper objectMapper;

    public GoldenDatasetLoader(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public GoldenDatasetLoader() {
        this(new ObjectMapper());
    }

    public List<GoldenDatasetEntry> loadDataset(String classpathResource) throws IOException {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(classpathResource)) {
            if (is == null) {
                throw new IllegalArgumentException("Resource not found: " + classpathResource);
            }
            return objectMapper.readValue(is, new TypeReference<List<GoldenDatasetEntry>>() {});
        }
    }
}
