package com.learning.ai.llmragwithspringai.config.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Configuration
@ConfigurationProperties(prefix = "rag.agent")
@Validated
public class AgentProperties {

    private boolean enabled = false;

    @Valid
    @NestedConfigurationProperty
    private Planner planner = new Planner();

    @Valid
    @NestedConfigurationProperty
    private Orchestrator orchestrator = new Orchestrator();

    @Valid
    @NestedConfigurationProperty
    private Retrieval retrieval = new Retrieval();

    @Valid
    @NestedConfigurationProperty
    private Memory memory = new Memory();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Planner getPlanner() {
        return planner;
    }

    public void setPlanner(Planner planner) {
        this.planner = planner;
    }

    public Orchestrator getOrchestrator() {
        return orchestrator;
    }

    public void setOrchestrator(Orchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    public Retrieval getRetrieval() {
        return retrieval;
    }

    public void setRetrieval(Retrieval retrieval) {
        this.retrieval = retrieval;
    }

    public Memory getMemory() {
        return memory;
    }

    public void setMemory(Memory memory) {
        this.memory = memory;
    }

    public static class Planner {
        @Min(1)
        private int maxSteps = 5;

        private Double temperature = 0.0;

        private String model;

        public int getMaxSteps() {
            return maxSteps;
        }

        public void setMaxSteps(int maxSteps) {
            this.maxSteps = maxSteps;
        }

        public Double getTemperature() {
            return temperature;
        }

        public void setTemperature(Double temperature) {
            this.temperature = temperature;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }
    }

    public static class Orchestrator {
        @Positive
        private long stepTimeoutSeconds = 30;

        private int maxToolCallsPerStep = 3;

        public long getStepTimeoutSeconds() {
            return stepTimeoutSeconds;
        }

        public void setStepTimeoutSeconds(long stepTimeoutSeconds) {
            this.stepTimeoutSeconds = stepTimeoutSeconds;
        }

        public int getMaxToolCallsPerStep() {
            return maxToolCallsPerStep;
        }

        public void setMaxToolCallsPerStep(int maxToolCallsPerStep) {
            this.maxToolCallsPerStep = maxToolCallsPerStep;
        }
    }

    public static class Retrieval {
        @Min(1)
        private int topK = 3;

        public int getTopK() {
            return topK;
        }

        public void setTopK(int topK) {
            this.topK = topK;
        }
    }

    public static class Memory {
        private boolean persistent = false;
        private long ttlSeconds = 3600;
        private int maxSize = 1000;

        public boolean isPersistent() {
            return persistent;
        }

        public void setPersistent(boolean persistent) {
            this.persistent = persistent;
        }

        public long getTtlSeconds() {
            return ttlSeconds;
        }

        public void setTtlSeconds(long ttlSeconds) {
            this.ttlSeconds = ttlSeconds;
        }

        public int getMaxSize() {
            return maxSize;
        }

        public void setMaxSize(int maxSize) {
            this.maxSize = maxSize;
        }
    }
}
