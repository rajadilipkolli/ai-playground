package com.learning.ai.config;

import com.learning.ai.service.Neo4jVectorStoreService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class Initializer implements CommandLineRunner {

    private final Neo4jVectorStoreService neo4jVectorStoreService;

    public Initializer(Neo4jVectorStoreService neo4jVectorStoreService) {
        this.neo4jVectorStoreService = neo4jVectorStoreService;
    }

    @Override
    public void run(String... args) {
        neo4jVectorStoreService.storeEmbeddings();
    }
}
