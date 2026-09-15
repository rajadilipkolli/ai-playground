package com.learning.ai.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "docling")
@Validated
public class DoclingProperties {

    @NotBlank
    private String serverUrl;

    /** Returns the base URL of the Docling server. */
    public String getServerUrl() {
        return serverUrl;
    }

    /** Sets the base URL of the Docling server. */
    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }
}
