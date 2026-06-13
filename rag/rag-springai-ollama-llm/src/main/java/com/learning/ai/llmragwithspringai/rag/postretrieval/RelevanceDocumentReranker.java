package com.learning.ai.llmragwithspringai.rag.postretrieval;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;

public class RelevanceDocumentReranker {

    private static final Logger log = LoggerFactory.getLogger(RelevanceDocumentReranker.class);
    private final int topK;

    public RelevanceDocumentReranker(int topK) {
        this.topK = topK;
    }

    public List<Document> rerank(List<Document> documents, Query query) {
        if (documents == null || documents.isEmpty()) {
            return documents;
        }

        String queryText = query.text().toLowerCase();
        Set<String> queryTokens = new HashSet<>(Arrays.asList(queryText.split("\\W+")));
        queryTokens.remove("");

        if (queryTokens.isEmpty()) {
            return documents.stream().limit(topK).toList();
        }

        List<Document> rerankedDocs = new ArrayList<>();

        for (Document doc : documents) {
            String docText = doc.getText().toLowerCase();
            Set<String> docTokens = new HashSet<>(Arrays.asList(docText.split("\\W+")));
            docTokens.remove("");

            long matchCount = queryTokens.stream().filter(docTokens::contains).count();
            double overlapRatio = (double) matchCount / queryTokens.size();

            Document updatedDoc = Document.builder()
                    .id(doc.getId())
                    .text(doc.getText())
                    .metadata(doc.getMetadata())
                    .build();

            updatedDoc.getMetadata().put("rerank_score", overlapRatio);
            rerankedDocs.add(updatedDoc);

            log.debug("Document {} overlap ratio: {}", doc.getId(), overlapRatio);
        }

        rerankedDocs.sort(Comparator.comparing((Document d) -> {
                    Object score = d.getMetadata().get("rerank_score");
                    return score instanceof Number ? ((Number) score).doubleValue() : 0.0;
                })
                .reversed());

        return rerankedDocs.stream().limit(topK).toList();
    }
}
