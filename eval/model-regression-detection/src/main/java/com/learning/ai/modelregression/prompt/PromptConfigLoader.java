package com.learning.ai.modelregression.prompt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.learning.ai.modelregression.model.PromptConfig;
import java.io.IOException;
import java.io.InputStream;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

@Component
public class PromptConfigLoader {

    private final ObjectMapper yamlMapper;
    private final String activeVersion;

    public PromptConfigLoader(@Value("${eval.prompt.version:}") String activeVersion) {
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
        this.activeVersion = activeVersion;
    }

    public PromptConfig loadConfig(String versionOverride) {
        String targetVersion = versionOverride;
        if (targetVersion == null || targetVersion.isBlank()) {
            targetVersion = activeVersion;
        }

        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath:/prompts/*.yaml");

            List<PromptConfig> configs = Stream.of(resources)
                    .map(this::loadFromFile)
                    .filter(Objects::nonNull)
                    .sorted(Comparator.comparing(PromptConfig::version).reversed())
                    .toList();

            if (configs.isEmpty()) {
                throw new IllegalStateException("No prompt configurations found in classpath:/prompts/");
            }

            if (targetVersion != null && !targetVersion.isBlank()) {
                String finalTargetVersion = targetVersion;
                return configs.stream()
                        .filter(c -> c.version().equals(finalTargetVersion))
                        .findFirst()
                        .orElseThrow(
                                () -> new IllegalArgumentException("Prompt version not found: " + finalTargetVersion));
            }

            // Return the latest version
            return configs.getFirst();

        } catch (IOException e) {
            throw new RuntimeException("Failed to load prompt configs", e);
        }
    }

    private PromptConfig loadFromFile(Resource resource) {
        try (InputStream is = resource.getInputStream()) {
            return yamlMapper.readValue(is, PromptConfig.class);
        } catch (IOException e) {
            return null; // Skip invalid or unreadable configs
        }
    }
}
