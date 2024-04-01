package com.learning.ai.controller;

import com.learning.ai.domain.response.AIChatResponse;
import com.learning.ai.service.PgVectorStoreService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
public class QueryController {

    private final PgVectorStoreService pgVectorStoreService;

    public QueryController(PgVectorStoreService pgVectorStoreService) {
        this.pgVectorStoreService = pgVectorStoreService;
    }

    @GetMapping("/query")
    AIChatResponse queryEmbeddedStore(@RequestParam String question) {
        return pgVectorStoreService.queryEmbeddingStore(question);
    }
}
