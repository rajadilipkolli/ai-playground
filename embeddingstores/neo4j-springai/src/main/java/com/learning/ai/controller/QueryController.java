package com.learning.ai.controller;

import com.learning.ai.model.request.AIChatRequest;
import com.learning.ai.model.response.AIChatResponse;
import com.learning.ai.service.Neo4jVectorStoreService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
public class QueryController {

    private final Neo4jVectorStoreService neo4jVectorStoreService;

    public QueryController(Neo4jVectorStoreService neo4jVectorStoreService) {
        this.neo4jVectorStoreService = neo4jVectorStoreService;
    }

    @PostMapping("/query")
    AIChatResponse queryEmbeddedStore(@RequestBody AIChatRequest aiChatRequest) {
        return neo4jVectorStoreService.queryEmbeddingStore(aiChatRequest.query());
    }
}
