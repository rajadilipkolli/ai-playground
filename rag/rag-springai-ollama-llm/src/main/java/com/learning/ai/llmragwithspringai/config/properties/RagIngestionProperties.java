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

        @Min(0)
        private long maxPdfSizeBytes = 20971520L; // 20MB default

        @Min(0)
        private int maxImagesPerPdf = 50;

        @Min(0)
        private long maxPixelsPerImage = 4000000L;

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

        public long getMaxPdfSizeBytes() {
            return maxPdfSizeBytes;
        }

        public void setMaxPdfSizeBytes(long maxPdfSizeBytes) {
            this.maxPdfSizeBytes = maxPdfSizeBytes;
        }

        public int getMaxImagesPerPdf() {
            return maxImagesPerPdf;
        }

        public void setMaxImagesPerPdf(int maxImagesPerPdf) {
            this.maxImagesPerPdf = maxImagesPerPdf;
        }

        public long getMaxPixelsPerImage() {
            return maxPixelsPerImage;
        }

        public void setMaxPixelsPerImage(long maxPixelsPerImage) {
            this.maxPixelsPerImage = maxPixelsPerImage;
        }
    }
}
