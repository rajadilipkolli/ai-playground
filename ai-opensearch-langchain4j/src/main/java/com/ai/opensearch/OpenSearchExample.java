package com.ai.opensearch;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.opensearch.OpenSearchEmbeddingStore;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
//import dev.langchain4j.store.embedding.opensearch.OpenSearchEmbeddingStore;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class OpenSearchExample {

    public static void main(String[] args) throws InterruptedException, IOException {

        EmbeddingStore<TextSegment> embeddingStore = OpenSearchEmbeddingStore.builder()
                .serverUrl("http://localhost:9200")
                .build();

        EmbeddingModel embeddingModel = new AllMiniLmL6V2EmbeddingModel();

//        TextSegment segment1 = TextSegment.from("I like football.");
//        Embedding embedding1 = embeddingModel.embed(segment1).content();
//        embeddingStore.add(embedding1, segment1);
//
//        TextSegment segment2 = TextSegment.from("The weather is good today.");
//        Embedding embedding2 = embeddingModel.embed(segment2).content();
//        embeddingStore.add(embedding2, segment2);
//
//        Thread.sleep(1000); // to be sure that embeddings were persisted

//        Resource input = new ClassPathResource("restaurants.json");
//        Path path = input.getFile().toPath();
//        List<String> restaurantArray = Files.readAllLines(path);
//        restaurantArray.forEach(s -> {
//            TextSegment segment = TextSegment.from(s);
//            Embedding embedding = embeddingModel.embed(segment).content();
//            embeddingStore.add(embedding, segment);
//        });
//
//        TimeUnit.SECONDS.sleep(5);

        Embedding queryEmbedding = embeddingModel.embed("What is your favourite cuisine with highest grade score?").content();
        List<EmbeddingMatch<TextSegment>> relevant = embeddingStore.findRelevant(queryEmbedding, 1);
        EmbeddingMatch<TextSegment> embeddingMatch = relevant.get(0);

        System.out.println(embeddingMatch.score()); // 0.8144289
        System.out.println(embeddingMatch.embedded().text()); // I like football.
    }
}
