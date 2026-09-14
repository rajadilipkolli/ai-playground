package com.learning.ai.model;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class IngestionJob {
    private final String jobId;
    private volatile String status; // QUEUED, PROCESSING, COMPLETED, FAILED
    private final AtomicInteger totalFiles;
    private final AtomicInteger processedFiles = new AtomicInteger();
    private final AtomicInteger failedFiles = new AtomicInteger();
    private volatile Instant startedAt;
    private volatile Instant completedAt;
    private final List<String> failedDocuments = new CopyOnWriteArrayList<>();

    public IngestionJob(String jobId, int totalFiles) {
        this.jobId = jobId;
        this.totalFiles = new AtomicInteger(totalFiles);
        this.status = "QUEUED";
        this.startedAt = Instant.now();
    }

    public String getJobId() { return jobId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getTotalFiles() { return totalFiles.get(); }
    public int getProcessedFiles() { return processedFiles.get(); }
    public void incrementProcessed() { processedFiles.incrementAndGet(); }
    public int getFailedFiles() { return failedFiles.get(); }
    public void incrementFailed() { failedFiles.incrementAndGet(); }
    public Instant getStartedAt() { return startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    public List<String> getFailedDocuments() { return failedDocuments; }
    public void addFailedDocument(String failure) { this.failedDocuments.add(failure); }
}
