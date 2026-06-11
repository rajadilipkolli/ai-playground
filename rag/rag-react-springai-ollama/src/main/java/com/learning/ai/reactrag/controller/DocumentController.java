package com.learning.ai.reactrag.controller;

import com.learning.ai.reactrag.model.response.IngestionResponse;
import com.learning.ai.reactrag.service.DocumentIngestionService;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentController.class);
    private final DocumentIngestionService ingestionService;

    public DocumentController(DocumentIngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping("/upload")
    public ResponseEntity<IngestionResponse> uploadDocument(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty() || file.getOriginalFilename() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new IngestionResponse(0, List.of("File is required and must not be empty")));
        }
        String filename = file.getOriginalFilename();
        if (filename != null && !filename.toLowerCase().matches(".*\\.(txt|pdf|md)$")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new IngestionResponse(
                            0, List.of("Invalid file format. Only .txt, .pdf, and .md are supported.")));
        }
        try {
            int chunkCount = ingestionService.ingestFile(file);
            return ResponseEntity.ok(new IngestionResponse(chunkCount, Collections.emptyList()));
        } catch (Exception e) {
            LOGGER.error("An unexpected error occurred during ingestion", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new IngestionResponse(0, List.of("An unexpected error occurred: " + e.getMessage())));
        }
    }
}
