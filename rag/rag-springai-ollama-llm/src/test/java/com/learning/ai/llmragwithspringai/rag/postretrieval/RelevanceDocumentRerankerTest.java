package com.learning.ai.llmragwithspringai.rag.postretrieval;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;

class RelevanceDocumentRerankerTest {

    private final RelevanceDocumentReranker reranker = new RelevanceDocumentReranker(2);

    @Test
    void shouldRerankDocumentsByKeywordOverlap() {
        Document doc1 = new Document("The quick brown fox");
        Document doc2 = new Document("A completely irrelevant sentence");
        Document doc3 = new Document("The quick brown fox jumps");

        List<Document> result = reranker.rerank(List.of(doc1, doc2, doc3), new Query("quick brown fox"));

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getText()).contains("quick brown fox");
        assertThat(result.get(1).getText()).contains("quick brown fox");
    }

    @Test
    void shouldLimitResultsToTopK() {
        Document doc1 = new Document("test");
        Document doc2 = new Document("test");
        Document doc3 = new Document("test");

        List<Document> result = reranker.rerank(List.of(doc1, doc2, doc3), new Query("test"));

        assertThat(result).hasSize(2);
    }

    @Test
    void shouldPopulateRerankScoreMetadata() {
        Document doc1 = new Document("test word");
        List<Document> result = reranker.rerank(List.of(doc1), new Query("test"));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getMetadata()).containsKey("rerank_score");
        assertThat((Double) result.get(0).getMetadata().get("rerank_score")).isGreaterThan(0.0);
    }
}
