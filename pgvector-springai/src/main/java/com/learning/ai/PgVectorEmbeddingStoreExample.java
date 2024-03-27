package com.learning.ai;

import com.learning.ai.config.VectorStoreComponent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PgVectorEmbeddingStoreExample implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(PgVectorEmbeddingStoreExample.class, args);
    }

    @Autowired
    VectorStoreComponent vectorStoreComponent;


    @Override
    public void run(String... args) throws Exception {
        vectorStoreComponent.storeAndRetrieveEmbeddings();
    }
}