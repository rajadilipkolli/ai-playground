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

        TextSegment segment1 = TextSegment.from("I like football.");
        Embedding embedding1 = embeddingModel.embed(segment1).content();
        embeddingStore.add(embedding1, segment1);

        TextSegment segment2 = TextSegment.from("The weather is good today.");
        Embedding embedding2 = embeddingModel.embed(segment2).content();
        embeddingStore.add(embedding2, segment2);

        TextSegment segment3 = TextSegment.from("I like cricket.");
        Embedding embedding3 = embeddingModel.embed(segment3).content();
        embeddingStore.add(embedding3, segment3);

        TextSegment segment4 = TextSegment.from("Cricket is my favourite sport.");
        Embedding embedding4 = embeddingModel.embed(segment4).content();
        embeddingStore.add(embedding4, segment4);

//        Thread.sleep(1000);

//        Resource input = new ClassPathResource("restaurants.json");
//        Path path = input.getFile().toPath();
//        List<String> restaurantArray = Files.readAllLines(path);
//        restaurantArray.forEach(s -> {
//            TextSegment segment = TextSegment.from(s);
//            Embedding embedding = embeddingModel.embed(segment).content();
//            embeddingStore.add(embedding, segment);
//        });
//
        TimeUnit.SECONDS.sleep(2); // to be sure that embeddings were persisted

        Embedding queryEmbedding = embeddingModel.embed("What is your favourite sport?").content();
        List<EmbeddingMatch<TextSegment>> relevant = embeddingStore.findRelevant(queryEmbedding, 1);
        EmbeddingMatch<TextSegment> embeddingMatch = relevant.get(0);

        System.out.println(embeddingMatch.score()); // 0.8805834
        System.out.println(embeddingMatch.embedded().text()); // Cricket is my favourite sport.

        queryEmbedding = embeddingModel.embed("Which sport do you love?").content();
        relevant = embeddingStore.findRelevant(queryEmbedding, 1);
        embeddingMatch = relevant.get(0);

        System.out.println(embeddingMatch.score()); // 0.8376416
        System.out.println(embeddingMatch.embedded().text()); // Cricket is my favourite sport.
    }
}
