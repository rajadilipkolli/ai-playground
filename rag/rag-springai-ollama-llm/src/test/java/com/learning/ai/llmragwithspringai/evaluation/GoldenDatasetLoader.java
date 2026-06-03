package com.learning.ai.llmragwithspringai.evaluation;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

public class GoldenDatasetLoader {

    private final JsonMapper jsonMapper;

    public GoldenDatasetLoader(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public GoldenDatasetLoader() {
        this(new JsonMapper());
    }

    public List<GoldenDatasetEntry> loadDataset(String classpathResource) throws IOException {
        try (InputStream is = new ClassPathResource(classpathResource).getInputStream()) {
            return jsonMapper.readValue(is, new TypeReference<List<GoldenDatasetEntry>>() {});
        }
    }
}
