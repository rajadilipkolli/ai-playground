package com.learning.ai.llmragwithspringai.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "rag.chunking")
public class RagChunkingProperties {

    private int size = 300;
    private int minSize = 100;
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
}
