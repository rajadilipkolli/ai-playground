package com.learning.ai.modelregression.dataset;

import java.io.IOException;
import java.io.InputStream;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class GoldenDatasetLoader {

    private final ObjectMapper objectMapper;

    public GoldenDatasetLoader(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public GoldenDataset loadDataset() {
        try {
            ClassPathResource resource = new ClassPathResource("golden-dataset.json");
            try (InputStream is = resource.getInputStream()) {
                return objectMapper.readValue(is, GoldenDataset.class);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load golden dataset", e);
        }
    }
}
