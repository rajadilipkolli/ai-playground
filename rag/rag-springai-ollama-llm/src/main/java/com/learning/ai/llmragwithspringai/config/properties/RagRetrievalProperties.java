package com.learning.ai.llmragwithspringai.config.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Configuration
@ConfigurationProperties(prefix = "rag.retrieval")
@Validated
public class RagRetrievalProperties {

    @Pattern(regexp = "^(keyword|vector|hybrid)$")
    private String mode = "hybrid";

    @Min(1)
    private int topK = 3;

    @DecimalMin("0.0")
    @DecimalMax("1.0")
    private double similarityThreshold = 0.6;

    @Valid
    @NestedConfigurationProperty
    private Keyword keyword = new Keyword();

    @Valid
    @NestedConfigurationProperty
    private Rrf rrf = new Rrf();

    @Valid
    @NestedConfigurationProperty
    private Hybrid hybrid = new Hybrid();

    @Valid
    @NestedConfigurationProperty
    private Rerank rerank = new Rerank();

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public int getTopK() {
        return topK;
    }

    public void setTopK(int topK) {
        this.topK = topK;
    }

    public double getSimilarityThreshold() {
        return similarityThreshold;
    }

    public void setSimilarityThreshold(double similarityThreshold) {
        this.similarityThreshold = similarityThreshold;
    }

    public Keyword getKeyword() {
        return keyword;
    }

    public void setKeyword(Keyword keyword) {
        this.keyword = keyword;
    }

    public Rrf getRrf() {
        return rrf;
    }

    public void setRrf(Rrf rrf) {
        this.rrf = rrf;
    }

    public Hybrid getHybrid() {
        return hybrid;
    }

    public void setHybrid(Hybrid hybrid) {
        this.hybrid = hybrid;
    }

    public Rerank getRerank() {
        return rerank;
    }

    public void setRerank(Rerank rerank) {
        this.rerank = rerank;
    }

    public static class Keyword {
        @Min(1)
        private int topK = 3;

        public int getTopK() {
            return topK;
        }

        public void setTopK(int topK) {
            this.topK = topK;
        }
    }

    public static class Rrf {
        @Min(1)
        private int k = 60;

        public int getK() {
            return k;
        }

        public void setK(int k) {
            this.k = k;
        }
    }

    public static class Hybrid {
        @Min(1)
        private int topK = 3;

        public int getTopK() {
            return topK;
        }

        public void setTopK(int topK) {
            this.topK = topK;
        }
    }

    public static class Rerank {
        private boolean enabled = false;

        @Min(1)
        private int topK = 3;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getTopK() {
            return topK;
        }

        public void setTopK(int topK) {
            this.topK = topK;
        }
    }
}
