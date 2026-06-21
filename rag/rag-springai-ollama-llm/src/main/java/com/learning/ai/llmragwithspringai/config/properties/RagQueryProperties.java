package com.learning.ai.llmragwithspringai.config.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Validated
@Configuration
@ConfigurationProperties(prefix = "rag.query")
public class RagQueryProperties {

    private boolean selfQueryingEnabled = false;

    @Nullable
    private String model;

    @Valid
    @NestedConfigurationProperty
    private MultiQuery multiquery = new MultiQuery();

    public boolean isSelfQueryingEnabled() {
        return selfQueryingEnabled;
    }

    public void setSelfQueryingEnabled(boolean selfQueryingEnabled) {
        this.selfQueryingEnabled = selfQueryingEnabled;
    }

    public @Nullable String getModel() {
        return model;
    }

    public void setModel(@Nullable String model) {
        this.model = model;
    }

    public MultiQuery getMultiquery() {
        return multiquery;
    }

    public void setMultiquery(MultiQuery multiquery) {
        this.multiquery = multiquery;
    }

    public static class MultiQuery {
        private boolean enabled = false;

        @Min(value = 1, message = "rag.query.multiquery.variations must be >= 1")
        private int variations = 3;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getVariations() {
            return variations;
        }

        public void setVariations(int variations) {
            this.variations = variations;
        }
    }
}
