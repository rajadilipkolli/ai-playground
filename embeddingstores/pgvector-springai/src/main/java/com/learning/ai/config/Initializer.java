package com.learning.ai.config;

import com.learning.ai.service.PgVectorStoreService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
class Initializer implements CommandLineRunner {

    private final PgVectorStoreService pgVectorStoreService;

    public Initializer(PgVectorStoreService pgVectorStoreService) {
        this.pgVectorStoreService = pgVectorStoreService;
    }

    @Override
    public void run(String... args) {
        pgVectorStoreService.storeEmbeddings();
    }
}
