package com.learning.ai.llmragwithspringai.rag.join;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.retrieval.join.DocumentJoiner;

public class RRFDocumentJoiner implements DocumentJoiner {

    private final int k;
    private final int topK;

    public RRFDocumentJoiner(int k, int topK) {
        this.k = k;
        this.topK = topK;
    }

    @Override
    public List<Document> join(Map<Query, List<List<Document>>> documentsForQuery) {
        Map<String, DocumentScore> rrfScores = new HashMap<>();

        for (List<List<Document>> listOfDocumentLists : documentsForQuery.values()) {
            for (List<Document> documentList : listOfDocumentLists) {
                int rank = 1;
                for (Document document : documentList) {
                    String docId = document.getId();
                    double rrfScore = 1.0 / (k + rank);

                    rrfScores.compute(docId, (id, existingScore) -> {
                        if (existingScore == null) {
                            return new DocumentScore(document, rrfScore);
                        } else {
                            existingScore.addScore(rrfScore);

                            // Merge retrieval_source metadata cleanly
                            Object currentSource =
                                    existingScore.getDocument().getMetadata().get("retrieval_source");
                            Object newSource = document.getMetadata().get("retrieval_source");
                            if (currentSource != null && newSource != null && !currentSource.equals(newSource)) {
                                Document currentDoc = existingScore.getDocument();
                                Document updatedDoc = Document.builder()
                                        .id(currentDoc.getId())
                                        .text(currentDoc.getText())
                                        .media(currentDoc.getMedia())
                                        .metadata(currentDoc.getMetadata())
                                        .metadata("retrieval_source", "both")
                                        .build();
                                existingScore.setDocument(updatedDoc);
                            }

                            return existingScore;
                        }
                    });
                    rank++;
                }
            }
        }

        List<DocumentScore> scoredDocuments = new ArrayList<>(rrfScores.values());
        // Sort descending by score
        scoredDocuments.sort((d1, d2) -> Double.compare(d2.getScore(), d1.getScore()));

        // Limit to topK
        int limit = Math.min(topK, scoredDocuments.size());
        List<Document> finalDocuments = new ArrayList<>(limit);

        for (int i = 0; i < limit; i++) {
            DocumentScore ds = scoredDocuments.get(i);
            Document doc = ds.getDocument();
            // Enrich metadata with rrf_score
            Document enrichedDoc = Document.builder()
                    .id(doc.getId())
                    .text(doc.getText())
                    .media(doc.getMedia())
                    .metadata(doc.getMetadata())
                    .metadata("rrf_score", ds.getScore())
                    .build();
            finalDocuments.add(enrichedDoc);
        }

        return finalDocuments;
    }

    private static class DocumentScore {
        private Document document;
        private double score;

        public DocumentScore(Document document, double initialScore) {
            this.document = document;
            this.score = initialScore;
        }

        public Document getDocument() {
            return document;
        }

        public void setDocument(Document document) {
            this.document = document;
        }

        public double getScore() {
            return score;
        }

        public void addScore(double additionalScore) {
            this.score += additionalScore;
        }
    }
}
