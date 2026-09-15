package com.learning.ai.modelregression.prompt;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.learning.ai.modelregression.model.PromptConfig;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;

@Component
public class PromptConfigLoader {

    private static final Logger log = LoggerFactory.getLogger(PromptConfigLoader.class);
    private final YAMLMapper yamlMapper;
    private final String activeVersion;
    private final Map<String, PromptConfig> cache = new LinkedHashMap<>();

    public PromptConfigLoader(@Value("${eval.prompt.version:}") String activeVersion) {
        this.yamlMapper = new YAMLMapper();
        this.activeVersion = activeVersion;
    }

    @PostConstruct
    public void init() {
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath:/prompts/*.yaml");

            List<PromptConfig> configs = Stream.of(resources)
                    .map(this::loadFromFile)
                    .filter(Objects::nonNull)
                    .sorted((c1, c2) -> Integer.compare(extractVersion(c2.version()), extractVersion(c1.version())))
                    .toList();

            for (PromptConfig c : configs) {
                cache.put(c.version(), c);
            }

            if (cache.isEmpty()) {
                throw new IllegalStateException("No prompt configurations found in classpath:/prompts/");
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load prompt configs", e);
        }
    }

    private int extractVersion(String version) {
        try {
            return Integer.parseInt(version.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public PromptConfig loadConfig(String versionOverride) {
        String targetVersion = versionOverride;
        if (targetVersion == null || targetVersion.isBlank()) {
            targetVersion = activeVersion;
        }

        if (targetVersion != null && !targetVersion.isBlank()) {
            PromptConfig config = cache.get(targetVersion);
            if (config == null) {
                throw new IllegalArgumentException("Prompt version not found: " + targetVersion);
            }
            return config;
        }

        return cache.values().iterator().next();
    }

    private PromptConfig loadFromFile(Resource resource) {
        try (InputStream is = resource.getInputStream()) {
            return yamlMapper.readValue(is, PromptConfig.class);
        } catch (JacksonException e) {
            log.warn("Failed to parse prompt config: {}", resource.getFilename(), e);
            return null;
        } catch (IOException e) {
            log.warn("Failed to read prompt config: {}", resource.getFilename(), e);
            return null;
        }
    }
}
