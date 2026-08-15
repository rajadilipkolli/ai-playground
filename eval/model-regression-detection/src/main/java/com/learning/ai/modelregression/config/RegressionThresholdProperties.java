package com.learning.ai.modelregression.config;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "eval.thresholds")
public class RegressionThresholdProperties {

    private double warningDeltaPercent = 3.0;
    private double criticalDeltaPercent = 8.0;
    private double driftThreshold = 85.0;

    @PostConstruct
    public void validate() {
        if (!Double.isFinite(warningDeltaPercent) || warningDeltaPercent < 0 || warningDeltaPercent > 100) {
            throw new IllegalArgumentException("warningDeltaPercent must be between 0 and 100");
        }
        if (!Double.isFinite(criticalDeltaPercent) || criticalDeltaPercent < 0 || criticalDeltaPercent > 100) {
            throw new IllegalArgumentException("criticalDeltaPercent must be between 0 and 100");
        }
        if (!Double.isFinite(driftThreshold) || driftThreshold < 0 || driftThreshold > 100) {
            throw new IllegalArgumentException("driftThreshold must be between 0 and 100");
        }
        if (criticalDeltaPercent <= warningDeltaPercent) {
            throw new IllegalArgumentException("criticalDeltaPercent must be greater than warningDeltaPercent");
        }
    }

    public double getWarningDeltaPercent() {
        return warningDeltaPercent;
    }

    public void setWarningDeltaPercent(double warningDeltaPercent) {
        this.warningDeltaPercent = warningDeltaPercent;
    }

    public double getCriticalDeltaPercent() {
        return criticalDeltaPercent;
    }

    public void setCriticalDeltaPercent(double criticalDeltaPercent) {
        this.criticalDeltaPercent = criticalDeltaPercent;
    }

    public double getDriftThreshold() {
        return driftThreshold;
    }

    public void setDriftThreshold(double driftThreshold) {
        this.driftThreshold = driftThreshold;
    }
}
