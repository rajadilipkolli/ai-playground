package com.learning.ai.llmragwithspringai.controller;

import com.learning.ai.llmragwithspringai.service.DataIndexerService;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/data/v1/")
class DataIndexController {

    private final DataIndexerService dataIndexerService;

    public DataIndexController(DataIndexerService dataIndexerService) {
        this.dataIndexerService = dataIndexerService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<String> load(@RequestPart("file") MultipartFile multipartFile) {
        try {
            this.dataIndexerService.loadData(multipartFile.getResource());
            return ResponseEntity.ok("Data indexed successfully!");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while indexing data: " + e.getMessage());
        }
    }

    @GetMapping("count")
    Map<String, Long> count() {
        return Map.of("count", dataIndexerService.count());
    }
}
