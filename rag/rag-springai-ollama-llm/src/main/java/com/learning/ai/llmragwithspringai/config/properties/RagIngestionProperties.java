package com.learning.ai.llmragwithspringai.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "rag.ingestion")
public class RagIngestionProperties {

    private final Pdf pdf = new Pdf();

    public Pdf getPdf() {
        return pdf;
    }

    public static class Pdf {
        private int bottomLinesToDelete = 3;
        private int topPagesToSkip = 1;

        public int getBottomLinesToDelete() {
            return bottomLinesToDelete;
        }

        public void setBottomLinesToDelete(int bottomLinesToDelete) {
            this.bottomLinesToDelete = bottomLinesToDelete;
        }

        public int getTopPagesToSkip() {
            return topPagesToSkip;
        }

        public void setTopPagesToSkip(int topPagesToSkip) {
            this.topPagesToSkip = topPagesToSkip;
        }
    }
}
