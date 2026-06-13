package com.learning.ai.controller;

import com.learning.ai.model.IngestionJob;
import com.learning.ai.repository.IngestionJobRepository;
import com.learning.ai.service.BatchIngestionService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/ingest")
public class BatchIngestionController {

    private final BatchIngestionService batchIngestionService;
    private final IngestionJobRepository jobRepository;

    public BatchIngestionController(BatchIngestionService batchIngestionService, IngestionJobRepository jobRepository) {
        this.batchIngestionService = batchIngestionService;
        this.jobRepository = jobRepository;
    }

    @PostMapping("/batch")
    public ResponseEntity<String> ingestBatch(@RequestParam("files") MultipartFile[] files) {
        if (files == null || files.length == 0) {
            return ResponseEntity.badRequest().body("No files provided");
        }
        
        String jobId = UUID.randomUUID().toString();
        IngestionJob job = new IngestionJob(jobId, files.length);
        jobRepository.save(job);
        
        batchIngestionService.processMultipartFiles(jobId, files);
        
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(jobId);
    }

    @PostMapping("/directory")
    public ResponseEntity<String> ingestDirectory(@RequestParam("path") String directoryPath) {
        Path path = Paths.get(directoryPath);
        if (!Files.exists(path) || !Files.isDirectory(path)) {
            return ResponseEntity.badRequest().body("Invalid directory path");
        }
        
        try (Stream<Path> paths = Files.walk(path)) {
            List<Path> files = paths.filter(Files::isRegularFile)
                                    .filter(p -> p.toString().toLowerCase().endsWith(".pdf"))
                                    .collect(Collectors.toList());
            
            if (files.isEmpty()) {
                return ResponseEntity.badRequest().body("No PDF files found in directory");
            }
            
            String jobId = UUID.randomUUID().toString();
            IngestionJob job = new IngestionJob(jobId, files.size());
            jobRepository.save(job);
            
            batchIngestionService.processDirectory(jobId, files);
            
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(jobId);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error reading directory");
        }
    }

    @GetMapping("/jobs/{jobId}")
    public ResponseEntity<IngestionJob> getJobStatus(@PathVariable String jobId) {
        return jobRepository.findById(jobId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/jobs/{jobId}/failures")
    public ResponseEntity<List<String>> getJobFailures(@PathVariable String jobId) {
        return jobRepository.findById(jobId)
                .map(job -> ResponseEntity.ok(job.getFailedDocuments()))
                .orElse(ResponseEntity.notFound().build());
    }
}
