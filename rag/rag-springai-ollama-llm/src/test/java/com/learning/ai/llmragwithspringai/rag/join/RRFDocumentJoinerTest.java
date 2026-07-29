package com.learning.ai.llmragwithspringai.rag.join;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;

class RRFDocumentJoinerTest {

    @Test
    void shouldCalculateRRFScoresCorrectly() {
        RRFDocumentJoiner joiner = new RRFDocumentJoiner(60, 10);
        Document doc1 = Document.builder()
                .id("1")
                .text("doc1")
                .metadata("retrieval_source", "vector")
                .build();
        Document doc2 = Document.builder()
                .id("2")
                .text("doc2")
                .metadata("retrieval_source", "keyword")
                .build();

        Map<Query, List<List<Document>>> map = new HashMap<>();
        map.put(new Query("test query"), Arrays.asList(List.of(doc1), List.of(doc2)));

        List<Document> result = joiner.join(map);
        assertThat(result).hasSize(2);

        Double score1 = (Double) result.stream()
                .filter(d -> d.getId().equals("1"))
                .findFirst()
                .get()
                .getMetadata()
                .get("rrf_score");
        Double score2 = (Double) result.stream()
                .filter(d -> d.getId().equals("2"))
                .findFirst()
                .get()
                .getMetadata()
                .get("rrf_score");

        assertThat(score1).isCloseTo(1.0 / 61, org.assertj.core.data.Offset.offset(0.0001));
        assertThat(score2).isCloseTo(1.0 / 61, org.assertj.core.data.Offset.offset(0.0001));
    }

    @Test
    void shouldMergeDocumentsAppearingInMultipleLists() {
        RRFDocumentJoiner joiner = new RRFDocumentJoiner(60, 10);
        Document doc1 = Document.builder()
                .id("1")
                .text("doc1")
                .metadata("retrieval_source", "vector")
                .build();
        Document doc1_keyword = Document.builder()
                .id("1")
                .text("doc1")
                .metadata("retrieval_source", "keyword")
                .build();

        Map<Query, List<List<Document>>> map = new HashMap<>();
        map.put(new Query("test query"), Arrays.asList(List.of(doc1), List.of(doc1_keyword)));

        List<Document> result = joiner.join(map);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getMetadata()).containsEntry("retrieval_source", "both");

        double expectedScore = (1.0 / 61) + (1.0 / 61);
        assertThat((Double) result.get(0).getMetadata().get("rrf_score"))
                .isCloseTo(expectedScore, org.assertj.core.data.Offset.offset(0.0001));
    }

    @Test
    void shouldLimitResultsToTopK() {
        RRFDocumentJoiner joiner = new RRFDocumentJoiner(60, 1);
        Document doc1 = Document.builder().id("1").text("doc1").build();
        Document doc2 = Document.builder().id("2").text("doc2").build();

        Map<Query, List<List<Document>>> map = new HashMap<>();
        // doc1 rank 1 in list 1, doc2 rank 2 in list 1
        map.put(new Query("test query"), Arrays.asList(List.of(doc1, doc2)));

        List<Document> result = joiner.join(map);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo("1"); // doc1 has rank 1, doc2 has rank 2
    }

    @Test
    void shouldHandleEmptyInput() {
        RRFDocumentJoiner joiner = new RRFDocumentJoiner(60, 10);
        assertThat(joiner.join(Collections.emptyMap())).isEmpty();
    }

    @Test
    void shouldHandleSingleRetrieverList() {
        RRFDocumentJoiner joiner = new RRFDocumentJoiner(60, 10);
        Document doc1 = Document.builder().id("1").text("doc1").build();
        Map<Query, List<List<Document>>> map = new HashMap<>();
        map.put(new Query("test query"), Arrays.asList(List.of(doc1)));

        List<Document> result = joiner.join(map);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo("1");
    }

    @Test
    void shouldHandleDisjointDocuments() {
        RRFDocumentJoiner joiner = new RRFDocumentJoiner(60, 10);
        Document doc1 = Document.builder().id("1").text("doc1").build();
        Document doc2 = Document.builder().id("2").text("doc2").build();
        Map<Query, List<List<Document>>> map = new HashMap<>();
        map.put(new Query("test query"), Arrays.asList(List.of(doc1), List.of(doc2)));

        List<Document> result = joiner.join(map);
        assertThat(result).hasSize(2);
        assertThat(result.stream().map(Document::getId)).containsExactlyInAnyOrder("1", "2");
    }

    @Test
    void shouldRejectInvalidKValue() {
        assertThatThrownBy(() -> new RRFDocumentJoiner(0, 10)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectInvalidTopKValue() {
        assertThatThrownBy(() -> new RRFDocumentJoiner(60, 0)).isInstanceOf(IllegalArgumentException.class);
    }
}
