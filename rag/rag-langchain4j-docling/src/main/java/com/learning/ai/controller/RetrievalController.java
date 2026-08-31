package com.learning.ai.controller;

import com.learning.ai.model.RetrievalRequest;
import com.learning.ai.model.RetrievalResponse;
import com.learning.ai.service.StructuredRetrievalService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class RetrievalController {

    private final StructuredRetrievalService retrievalService;

    public RetrievalController(StructuredRetrievalService retrievalService) {
        this.retrievalService = retrievalService;
    }

    @PostMapping("/retrieve")
    @Operation(summary = "Structured Retrieval", description = "Query document chunks with vector similarity and metadata filters.")
    public ResponseEntity<RetrievalResponse> retrieve(@RequestBody RetrievalRequest request) {
        if (request.query() == null || request.query().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        
        RetrievalResponse response = retrievalService.retrieve(request);
        return ResponseEntity.ok(response);
    }
}
