package com.learning.ai.llmragwithspringai.config.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Configuration
@ConfigurationProperties(prefix = "rag.ingestion")
@Validated
public class RagIngestionProperties {

    @Valid
    @NestedConfigurationProperty
    private final Pdf pdf = new Pdf();

    public Pdf getPdf() {
        return pdf;
    }

    public static class Pdf {
        @Min(0)
        private int bottomLinesToDelete = 3;

        @Min(0)
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
