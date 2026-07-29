package com.learning.ai.llmragwithspringai.agent.impl;

import com.learning.ai.llmragwithspringai.model.response.RetrievalDiagnostic;
import java.util.List;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;

public class RetrievalAction {
    private final DocumentRetriever documentRetriever;
    private final int topK;

    public RetrievalAction(DocumentRetriever documentRetriever, int topK) {
        this.documentRetriever = documentRetriever;
        this.topK = topK;
    }

    public List<Document> retrieve(String queryText) {
        org.springframework.ai.rag.Query query = new org.springframework.ai.rag.Query(queryText);
        List<Document> docs = documentRetriever.retrieve(query);
        if (docs.size() > topK) {
            return docs.subList(0, topK);
        }
        return docs;
    }

    public static List<RetrievalDiagnostic> mapToDiagnostics(List<Document> docs) {
        return docs.stream()
                .map(d -> {
                    Object vectorScore = d.getMetadata().get("distance");
                    Object keywordScore = d.getMetadata().get("ts_rank");
                    Object rrfScoreObj = d.getMetadata().get("rrf_score");
                    Object sourceObj = d.getMetadata().get("retrieval_source");

                    double originalScore = 0.0;
                    if (vectorScore instanceof Number n) originalScore = n.doubleValue();
                    else if (keywordScore instanceof Number n) originalScore = n.doubleValue();

                    String text = d.getText() != null ? d.getText() : "";
                    Double rrfScore = rrfScoreObj instanceof Number n ? n.doubleValue() : 0.0;
                    String source = sourceObj instanceof String s ? s : "unknown";

                    return new RetrievalDiagnostic(text, originalScore, rrfScore, source);
                })
                .toList();
    }
}
