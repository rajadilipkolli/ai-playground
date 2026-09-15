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

    /** Creates a queued ingestion job for the expected number of files. */
    public IngestionJob(String jobId, int totalFiles) {
        this.jobId = jobId;
        this.totalFiles = new AtomicInteger(totalFiles);
        this.status = "QUEUED";
        this.startedAt = Instant.now();
    }

    /** Returns the job identifier. */
    public String getJobId() {
        return jobId;
    }
    /** Returns the current job status. */
    public String getStatus() {
        return status;
    }
    /** Updates the current job status. */
    public void setStatus(String status) {
        this.status = status;
    }
    /** Returns the expected number of files. */
    public int getTotalFiles() {
        return totalFiles.get();
    }
    /** Returns the number of successfully processed files. */
    public int getProcessedFiles() {
        return processedFiles.get();
    }
    /** Increments the successfully processed file count. */
    public void incrementProcessed() {
        processedFiles.incrementAndGet();
    }
    /** Returns the number of failed files. */
    public int getFailedFiles() {
        return failedFiles.get();
    }
    /** Increments the failed file count. */
    public void incrementFailed() {
        failedFiles.incrementAndGet();
    }
    /** Returns the time at which the queued job was created. */
    public Instant getStartedAt() {
        return startedAt;
    }
    /** Returns the time at which processing completed. */
    public Instant getCompletedAt() {
        return completedAt;
    }
    /** Records the time at which processing completed. */
    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }
    /** Returns the recorded document failure messages. */
    public List<String> getFailedDocuments() {
        return failedDocuments;
    }
    /** Adds a document failure message. */
    public void addFailedDocument(String failure) {
        this.failedDocuments.add(failure);
    }
}
