package com.learning.ai.llmragwithspringai.config.properties;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.util.regex.PatternSyntaxException;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Configuration
@ConfigurationProperties(prefix = "rag.chunking")
@Validated
public class RagChunkingProperties {

    @Positive(message = "size must be positive")
    private int size = 300;

    @Positive(message = "minSize must be positive")
    private int minSize = 100;

    @Pattern(regexp = "^(token|section)$", message = "strategy must be 'token' or 'section'")
    private String strategy = "token";

    private String sectionPattern = "(^#+\\s+.*$)|(\\n\\n)";

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getMinSize() {
        return minSize;
    }

    public void setMinSize(int minSize) {
        this.minSize = minSize;
    }

    public String getStrategy() {
        return strategy;
    }

    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }

    public String getSectionPattern() {
        return sectionPattern;
    }

    public void setSectionPattern(String sectionPattern) {
        this.sectionPattern = sectionPattern;
    }

    @AssertTrue(message = "size must be greater than or equal to minSize")
    public boolean isSizeValid() {
        return size >= minSize;
    }

    @AssertTrue(message = "sectionPattern must be a valid regular expression")
    public boolean isSectionPatternValid() {
        try {
            java.util.regex.Pattern.compile(this.sectionPattern);
            return true;
        } catch (PatternSyntaxException e) {
            return false;
        }
    }
}
