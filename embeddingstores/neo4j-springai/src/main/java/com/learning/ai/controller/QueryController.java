package com.learning.ai.controller;

import com.learning.ai.model.response.AIChatResponse;
import com.learning.ai.service.Neo4jVectorStoreService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
public class QueryController {

    private final Neo4jVectorStoreService neo4jVectorStoreService;

    public QueryController(Neo4jVectorStoreService neo4jVectorStoreService) {
        this.neo4jVectorStoreService = neo4jVectorStoreService;
    }

    @GetMapping("/query")
    AIChatResponse queryEmbeddedStore(@RequestParam String question) {
        return neo4jVectorStoreService.queryEmbeddingStore(question);
    }
}
