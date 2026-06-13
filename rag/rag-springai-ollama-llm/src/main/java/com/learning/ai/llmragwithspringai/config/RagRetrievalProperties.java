package com.learning.ai.llmragwithspringai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "rag.retrieval")
public class RagRetrievalProperties {

    private String mode = "hybrid";
    private int topK = 3;
    private double similarityThreshold = 0.6;

    private Keyword keyword = new Keyword();
    private Rrf rrf = new Rrf();
    private Hybrid hybrid = new Hybrid();
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
        private int topK = 3;

        public int getTopK() {
            return topK;
        }

        public void setTopK(int topK) {
            this.topK = topK;
        }
    }

    public static class Rrf {
        private int k = 60;

        public int getK() {
            return k;
        }

        public void setK(int k) {
            this.k = k;
        }
    }

    public static class Hybrid {
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
