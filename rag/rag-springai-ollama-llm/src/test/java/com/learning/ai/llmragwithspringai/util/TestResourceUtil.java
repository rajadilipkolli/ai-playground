package com.learning.ai.llmragwithspringai.util;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.Resource;

public class TestResourceUtil {

    private TestResourceUtil() {
        // Utility class
    }

    public static Resource createMockResource(String filename, String content) {
        Resource resource = mock(Resource.class);
        lenient().when(resource.getFilename()).thenReturn(filename);
        try {
            lenient()
                    .when(resource.getInputStream())
                    .thenAnswer(inv -> new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return resource;
    }
}
