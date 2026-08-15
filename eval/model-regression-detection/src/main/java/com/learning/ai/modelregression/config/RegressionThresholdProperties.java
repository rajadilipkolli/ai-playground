package com.learning.ai.modelregression.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "eval.thresholds")
public class RegressionThresholdProperties {

    private double warningDeltaPercent = 3.0;
    private double criticalDeltaPercent = 8.0;
    private double driftThreshold = 85.0;

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
