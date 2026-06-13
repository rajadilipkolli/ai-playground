package com.learning.ai.llmragwithspringai.controller;

import com.learning.ai.llmragwithspringai.model.response.IngestionResult;
import com.learning.ai.llmragwithspringai.model.response.IngestionStatus;
import com.learning.ai.llmragwithspringai.service.DataIndexerService;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/data/v1/")
class DataIndexController {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataIndexController.class);

    private final DataIndexerService dataIndexerService;
    private final CacheManager cacheManager;

    public DataIndexController(
            DataIndexerService dataIndexerService, ObjectProvider<CacheManager> cacheManagerProvider) {
        this.dataIndexerService = dataIndexerService;
        this.cacheManager = cacheManagerProvider.getIfAvailable();
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<?> load(
            @RequestPart("file") MultipartFile multipartFile,
            @RequestParam(value = "documentType", required = false) String documentType,
            @RequestParam(value = "owner", required = false) String owner,
            @RequestParam(value = "category", required = false) String category) {
        if (multipartFile.isEmpty()) {
            return ResponseEntity.badRequest().body("Uploaded file is empty");
        }
        try {
            IngestionResult result =
                    this.dataIndexerService.loadData(multipartFile.getResource(), documentType, owner, category);
            if (this.cacheManager != null && result.status() != IngestionStatus.SKIPPED_DUPLICATE) {
                Cache cache = this.cacheManager.getCache("retrieval-cache");
                if (cache != null) {
                    cache.clear();
                }
            }
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            LOGGER.error("Error indexing data", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An internal server error occurred while indexing data");
        }
    }

    @GetMapping("count")
    Map<String, Long> count() {
        return Map.of("count", dataIndexerService.count());
    }

    @DeleteMapping("cache")
    ResponseEntity<?> clearCache() {
        if (this.cacheManager != null) {
            Cache cache = this.cacheManager.getCache("retrieval-cache");
            if (cache != null) {
                cache.clear();
                return ResponseEntity.ok(Map.of("message", "Cache cleared successfully"));
            }
        }
        return ResponseEntity.ok(Map.of("message", "No cache configured"));
    }
}
