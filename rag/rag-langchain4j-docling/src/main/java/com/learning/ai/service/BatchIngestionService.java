package com.learning.ai.service;

import com.learning.ai.benchmark.IngestionBenchmark;
import com.learning.ai.model.IngestionJob;
import com.learning.ai.repository.IngestionJobRepository;
import com.learning.ai.util.ContentHashUtil;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import jakarta.annotation.PreDestroy;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class BatchIngestionService {

    private static final Logger log = LoggerFactory.getLogger(BatchIngestionService.class);

    private final IngestionJobRepository jobRepository;
    private final DocumentParserService documentParserService;
    private final StructureAwareChunker chunker;
    private final MetadataEnricher metadataEnricher;
    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final JdbcTemplate jdbcTemplate;
    private final ExecutorService executorService;
    private final IngestionBenchmark ingestionBenchmark;

    /** Creates the ingestion service and its fixed-size worker pool. */
    public BatchIngestionService(
            IngestionJobRepository jobRepository,
            DocumentParserService documentParserService,
            StructureAwareChunker chunker,
            MetadataEnricher metadataEnricher,
            EmbeddingModel embeddingModel,
            EmbeddingStore<TextSegment> embeddingStore,
            JdbcTemplate jdbcTemplate,
            IngestionBenchmark ingestionBenchmark,
            @Value("${ingestion.parallelism:4}") int parallelism) {
        this.jobRepository = jobRepository;
        this.documentParserService = documentParserService;
        this.chunker = chunker;
        this.metadataEnricher = metadataEnricher;
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
        this.jdbcTemplate = jdbcTemplate;
        this.ingestionBenchmark = ingestionBenchmark;
        this.executorService = Executors.newFixedThreadPool(parallelism);
    }

    /** Processes uploaded files asynchronously for an existing ingestion job. */
    public void processMultipartFiles(String jobId, MultipartFile[] files) {
        IngestionJob job = jobRepository.findById(jobId).orElseThrow();
        job.setStatus("PROCESSING");
        ingestionBenchmark.startBatch(jobId, files.length);

        CompletableFuture<?>[] futures = new CompletableFuture<?>[files.length];
        for (int i = 0; i < files.length; i++) {
            MultipartFile file = files[i];
            futures[i] = CompletableFuture.runAsync(
                    () -> {
                        try {
                            byte[] bytes = file.getBytes();
                            Resource resource = new ByteArrayResource(bytes);
                            String filename =
                                    file.getOriginalFilename() != null ? file.getOriginalFilename() : "document.pdf";
                            processSingleDocument(jobId, filename, null, resource);
                            job.incrementProcessed();
                        } catch (Exception e) {
                            log.error("Failed to process file {}", file.getOriginalFilename(), e);
                            job.incrementFailed();
                            job.addFailedDocument(file.getOriginalFilename() + ": " + e.getMessage());
                        }
                    },
                    executorService);
        }

        finalizeJob(job, futures);
    }

    /** Processes filesystem documents asynchronously for an existing job. */
    public void processDirectory(String jobId, List<Path> files) {
        IngestionJob job = jobRepository.findById(jobId).orElseThrow();
        job.setStatus("PROCESSING");
        ingestionBenchmark.startBatch(jobId, files.size());

        CompletableFuture<?>[] futures = new CompletableFuture<?>[files.size()];
        for (int i = 0; i < files.size(); i++) {
            Path file = files.get(i);
            futures[i] = CompletableFuture.runAsync(
                    () -> {
                        try {
                            byte[] bytes = Files.readAllBytes(file);
                            Resource resource = new ByteArrayResource(bytes);
                            processSingleDocument(
                                    jobId,
                                    file.getFileName().toString(),
                                    file.toRealPath().toString(),
                                    resource);
                            job.incrementProcessed();
                        } catch (Exception e) {
                            log.error("Failed to process file {}", file.getFileName(), e);
                            job.incrementFailed();
                            job.addFailedDocument(file.getFileName().toString() + ": " + e.getMessage());
                        }
                    },
                    executorService);
        }

        finalizeJob(job, futures);
    }

    /** Completes a job after all of its document tasks finish. */
    private void finalizeJob(IngestionJob job, CompletableFuture<?>[] futures) {
        CompletableFuture.allOf(futures).whenComplete((res, ex) -> {
            job.setStatus(job.getFailedFiles() == job.getTotalFiles() ? "FAILED" : "COMPLETED");
            job.setCompletedAt(Instant.now());
            ingestionBenchmark.endBatch(job.getJobId());
        });
    }

    /**
     * Ingests one document unless its source and content hash already exist, replacing stale chunks for
     * the same source before storing new ones.
     */
    private void processSingleDocument(String batchId, String filename, String sourcePath, Resource resource)
            throws Exception {
        long startTime = System.currentTimeMillis();
        String contentHash = ContentHashUtil.calculateHash(resource);
        String documentKey = sourcePath != null ? sourcePath : "upload:" + filename + ":" + contentHash;

        // Deduplication check
        List<String> existingHashes = jdbcTemplate.queryForList(
                "SELECT DISTINCT metadata->>'content_hash' FROM vector_store WHERE metadata->>'source_path' = ?",
                String.class,
                documentKey);

        if (existingHashes.contains(contentHash)) {
            log.info("File {} with hash {} already exists. Skipping.", filename, contentHash);
            return;
        }

        boolean hasStaleChunks = !existingHashes.isEmpty();

        try (InputStream is = resource.getInputStream()) {
            Document document = documentParserService.parse(is);
            List<TextSegment> segments = chunker.chunk(document);
            segments = metadataEnricher.enrich(segments, filename, documentKey, contentHash);

            // Embed and store
            var embeddings = embeddingModel.embedAll(segments).content();
            embeddingStore.addAll(embeddings, segments);

            if (hasStaleChunks) {
                log.info("File {} updated with new hash. Deleting old chunks.", filename);
                jdbcTemplate.update(
                        "DELETE FROM vector_store WHERE metadata->>'source_path' = ? AND (metadata->>'content_hash' != ? OR metadata->>'content_hash' IS NULL)",
                        documentKey,
                        contentHash);
            }

            log.info("Successfully ingested file {} into {} chunks", filename, segments.size());
            long duration = System.currentTimeMillis() - startTime;
            ingestionBenchmark.recordDocumentProcessed(batchId, duration);
        }
    }

    /** Gracefully stops the ingestion worker pool during application shutdown. */
    @PreDestroy
    void shutdownExecutor() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                log.warn("Ingestion executor did not stop in time; forcing shutdown");
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
