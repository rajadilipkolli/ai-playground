package com.ai.opensearch;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.opensearch.OpenSearchEmbeddingStore;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class OpenSearchExample {

    private static final int batchSize = 200;

    public static void main(String[] args) throws IOException, InterruptedException, URISyntaxException {

        EmbeddingStore<TextSegment> embeddingStore = OpenSearchEmbeddingStore.builder()
                .serverUrl("http://localhost:9200")
                .indexName("langchain4j_restaurant")
                .build();

        EmbeddingModel embeddingModel = new AllMiniLmL6V2EmbeddingModel();

        // equal to 0 to load data
        boolean loadData = args.length == 0;
        if (loadData) {
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

            URL fileUrl = OpenSearchExample.class.getResource("/restaurants.json");
            Path path = Paths.get(fileUrl.toURI());

//        Document document = FileSystemDocumentLoader.loadDocument(path, new TextDocumentParser());
//        DocumentSplitter splitter = DocumentSplitters.recursive(600, 0);
//        List<TextSegment> segments = splitter.split(document);

             // Read lines from the file in parallel
             List<String> lines = Files.lines(path).parallel().toList();

             // Convert lines to text segments in parallel
             List<TextSegment> segments = lines.parallelStream().map(TextSegment::from).toList();
 
            // Split the data into batches
            List<List<TextSegment>> batches = splitIntoBatches(segments, batchSize);
            // Process and save each batch
            for (int i = 0; i < batches.size(); i++) {
                List<Embedding> embeddings = embeddingModel.embedAll(batches.get(i)).content();
                embeddingStore.addAll(embeddings, batches.get(i));
                System.out.println("Saved batch " + i);
            }

            TimeUnit.SECONDS.sleep(5); // to be sure that embeddings were persisted
        }

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

        queryEmbedding = embeddingModel.embed("Which is the restaurant with highest rating?").content();
        relevant = embeddingStore.findRelevant(queryEmbedding, 1);
        embeddingMatch = relevant.get(0);

        System.out.println(embeddingMatch.score()); // 0.64560163
        System.out.println(embeddingMatch.embedded().text()); // "restaurant_id": "40371727"
    }

    // Split a list into batches
    public static <T> List<List<T>> splitIntoBatches(List<T> data, int batchSize) {
        List<List<T>> batches = new ArrayList<>();
        for (int i = 0; i < data.size(); i += batchSize) {
            batches.add(data.subList(i, Math.min(i + batchSize, data.size())));
        }
        return batches;
    }

}
