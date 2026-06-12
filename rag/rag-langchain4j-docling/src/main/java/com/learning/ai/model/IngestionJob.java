package com.learning.ai.model;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class IngestionJob {
    private String jobId;
    private String status; // QUEUED, PROCESSING, COMPLETED, FAILED
    private int totalFiles;
    private int processedFiles;
    private int failedFiles;
    private Instant startedAt;
    private Instant completedAt;
    private List<String> failedDocuments = new CopyOnWriteArrayList<>();

    public IngestionJob(String jobId, int totalFiles) {
        this.jobId = jobId;
        this.totalFiles = totalFiles;
        this.status = "QUEUED";
        this.processedFiles = 0;
        this.failedFiles = 0;
        this.startedAt = Instant.now();
    }

    public String getJobId() { return jobId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getTotalFiles() { return totalFiles; }
    public int getProcessedFiles() { return processedFiles; }
    public void incrementProcessed() { this.processedFiles++; }
    public int getFailedFiles() { return failedFiles; }
    public void incrementFailed() { this.failedFiles++; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    public List<String> getFailedDocuments() { return failedDocuments; }
    public void addFailedDocument(String failure) { this.failedDocuments.add(failure); }
}
