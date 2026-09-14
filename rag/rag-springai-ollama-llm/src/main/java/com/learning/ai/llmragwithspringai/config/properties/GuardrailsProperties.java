package com.learning.ai.llmragwithspringai.config.properties;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "guardrails")
public class GuardrailsProperties {

    private List<String> sensitiveWords = new ArrayList<>();
    private String failureMessage = "";

    @NestedConfigurationProperty
    private Logging logging = new Logging();

    public List<String> getSensitiveWords() {
        return sensitiveWords;
    }

    public void setSensitiveWords(List<String> sensitiveWords) {
        this.sensitiveWords = sensitiveWords;
    }

    public String getFailureMessage() {
        return failureMessage;
    }

    public void setFailureMessage(String failureMessage) {
        this.failureMessage = failureMessage;
    }

    public Logging getLogging() {
        return logging;
    }

    public void setLogging(Logging logging) {
        this.logging = logging;
    }

    public static class Logging {
        private boolean enabled;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
