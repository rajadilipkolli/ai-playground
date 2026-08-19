package com.learning.ai.service;

import com.learning.ai.model.IngestionJob;
import com.learning.ai.repository.IngestionJobRepository;
import com.learning.ai.util.ContentHashUtil;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.model.embedding.EmbeddingModel;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.learning.ai.benchmark.IngestionBenchmark;

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

    public void processMultipartFiles(String jobId, MultipartFile[] files) {
        IngestionJob job = jobRepository.findById(jobId).orElseThrow();
        job.setStatus("PROCESSING");
        ingestionBenchmark.startBatch(jobId, files.length);

        CompletableFuture<?>[] futures = new CompletableFuture<?>[files.length];
        for (int i = 0; i < files.length; i++) {
            MultipartFile file = files[i];
            futures[i] = CompletableFuture.runAsync(() -> {
                try {
                    byte[] bytes = file.getBytes();
                    Resource resource = new ByteArrayResource(bytes);
                    processSingleDocument(file.getOriginalFilename(), resource);
                    job.incrementProcessed();
                } catch (Exception e) {
                    log.error("Failed to process file {}", file.getOriginalFilename(), e);
                    job.incrementFailed();
                    job.addFailedDocument(file.getOriginalFilename() + ": " + e.getMessage());
                }
            }, executorService);
        }

        finalizeJob(job, futures);
    }

    public void processDirectory(String jobId, List<Path> files) {
        IngestionJob job = jobRepository.findById(jobId).orElseThrow();
        job.setStatus("PROCESSING");
        ingestionBenchmark.startBatch(jobId, files.size());

        CompletableFuture<?>[] futures = new CompletableFuture<?>[files.size()];
        for (int i = 0; i < files.size(); i++) {
            Path file = files.get(i);
            futures[i] = CompletableFuture.runAsync(() -> {
                try {
                    byte[] bytes = Files.readAllBytes(file);
                    Resource resource = new ByteArrayResource(bytes);
                    processSingleDocument(file.getFileName().toString(), resource);
                    job.incrementProcessed();
                } catch (Exception e) {
                    log.error("Failed to process file {}", file.getFileName(), e);
                    job.incrementFailed();
                    job.addFailedDocument(file.getFileName().toString() + ": " + e.getMessage());
                }
            }, executorService);
        }

        finalizeJob(job, futures);
    }

    private void finalizeJob(IngestionJob job, CompletableFuture<?>[] futures) {
        CompletableFuture.allOf(futures).whenComplete((res, ex) -> {
            job.setStatus(job.getFailedFiles() == job.getTotalFiles() ? "FAILED" : "COMPLETED");
            job.setCompletedAt(Instant.now());
            ingestionBenchmark.endBatch(job.getJobId());
        });
    }

    private void processSingleDocument(String filename, Resource resource) throws Exception {
        long startTime = System.currentTimeMillis();
        String contentHash = ContentHashUtil.calculateHash(resource);

        // Deduplication check
        List<String> existingHashes = jdbcTemplate.queryForList(
                "SELECT DISTINCT metadata->>'content_hash' FROM vector_store WHERE metadata->>'source_filename' = ?",
                String.class, filename);

        if (existingHashes.contains(contentHash)) {
            log.info("File {} with hash {} already exists. Skipping.", filename, contentHash);
            return;
        } else if (!existingHashes.isEmpty()) {
            log.info("File {} exists with different hash. Deleting old chunks.", filename);
            jdbcTemplate.update("DELETE FROM vector_store WHERE metadata->>'source_filename' = ?", filename);
        }

        try (InputStream is = resource.getInputStream()) {
            Document document = documentParserService.parse(is);
            List<TextSegment> segments = chunker.chunk(document);
            segments = metadataEnricher.enrich(segments, filename, contentHash);
            
            // Embed and store
            var embeddings = embeddingModel.embedAll(segments).content();
            embeddingStore.addAll(embeddings, segments);
            log.info("Successfully ingested file {} into {} chunks", filename, segments.size());
            long duration = System.currentTimeMillis() - startTime;
            ingestionBenchmark.recordDocumentProcessed(duration);
        }
    }
}
