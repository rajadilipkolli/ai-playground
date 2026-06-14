package com.learning.ai.llmragwithspringai.rag.retrieval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.learning.ai.llmragwithspringai.rag.join.RRFDocumentJoiner;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;

@ExtendWith(MockitoExtension.class)
class HybridDocumentRetrieverTest {

    @Mock
    private DocumentRetriever vectorRetriever;

    @Mock
    private KeywordDocumentRetriever keywordRetriever;

    @Mock
    private RRFDocumentJoiner documentJoiner;

    private final Executor directExecutor = Runnable::run;

    @Test
    void shouldCombineResultsFromBothRetrievers() {
        HybridDocumentRetriever retriever =
                new HybridDocumentRetriever(vectorRetriever, keywordRetriever, documentJoiner, directExecutor);

        Query query = new Query("test");
        Document doc1 = Document.builder().id("1").text("vector").build();
        Document doc2 = Document.builder().id("2").text("keyword").build();

        when(vectorRetriever.retrieve(query)).thenReturn(List.of(doc1));
        when(keywordRetriever.retrieve(query)).thenReturn(List.of(doc2));

        Document joinedDoc = Document.builder().id("3").text("joined").build();
        when(documentJoiner.join(any(Map.class))).thenReturn(List.of(joinedDoc));

        List<Document> results = retriever.retrieve(query);

        assertThat(results).containsExactly(joinedDoc);
        verify(documentJoiner).join(argThat(map -> {
            List<List<Document>> lists = map.get(query);
            return lists.size() == 2;
        }));
    }

    @Test
    void shouldHandleVectorRetrieverFailure() {
        HybridDocumentRetriever retriever =
                new HybridDocumentRetriever(vectorRetriever, keywordRetriever, documentJoiner, directExecutor);

        Query query = new Query("test");
        Document doc2 = Document.builder().id("2").text("keyword").build();

        when(vectorRetriever.retrieve(query)).thenThrow(new RuntimeException("vector failure"));
        when(keywordRetriever.retrieve(query)).thenReturn(List.of(doc2));

        Document joinedDoc = Document.builder().id("3").text("joined").build();
        when(documentJoiner.join(any(Map.class))).thenReturn(List.of(joinedDoc));

        List<Document> results = retriever.retrieve(query);

        assertThat(results).containsExactly(joinedDoc);
        verify(documentJoiner).join(argThat(map -> {
            List<List<Document>> lists = map.get(query);
            return lists.size() == 1; // only keyword results
        }));
    }

    @Test
    void shouldHandleKeywordRetrieverFailure() {
        HybridDocumentRetriever retriever =
                new HybridDocumentRetriever(vectorRetriever, keywordRetriever, documentJoiner, directExecutor);

        Query query = new Query("test");
        Document doc1 = Document.builder().id("1").text("vector").build();

        when(vectorRetriever.retrieve(query)).thenReturn(List.of(doc1));
        when(keywordRetriever.retrieve(query)).thenThrow(new RuntimeException("keyword failure"));

        Document joinedDoc = Document.builder().id("3").text("joined").build();
        when(documentJoiner.join(any(Map.class))).thenReturn(List.of(joinedDoc));

        List<Document> results = retriever.retrieve(query);

        assertThat(results).containsExactly(joinedDoc);
        verify(documentJoiner).join(argThat(map -> {
            List<List<Document>> lists = map.get(query);
            return lists.size() == 1; // only vector results
        }));
    }

    @Test
    void shouldHandleBothRetrieversFailure() {
        HybridDocumentRetriever retriever =
                new HybridDocumentRetriever(vectorRetriever, keywordRetriever, documentJoiner, directExecutor);

        Query query = new Query("test");

        when(vectorRetriever.retrieve(query)).thenThrow(new RuntimeException("vector failure"));
        when(keywordRetriever.retrieve(query)).thenThrow(new RuntimeException("keyword failure"));

        when(documentJoiner.join(any(Map.class))).thenReturn(Collections.emptyList());

        List<Document> results = retriever.retrieve(query);

        assertThat(results).isEmpty();
        verify(documentJoiner).join(argThat(map -> {
            List<List<Document>> lists = map.get(query);
            return lists.isEmpty(); // both failed
        }));
    }

    @Test
    void shouldPropagateFilterContextToAsyncThreads() {
        AtomicReference<Filter.Expression> vectorFilter = new AtomicReference<>();
        DocumentRetriever customVectorRetriever = q -> {
            vectorFilter.set(FilterContext.getFilterExpression());
            return Collections.emptyList();
        };

        HybridDocumentRetriever retriever =
                new HybridDocumentRetriever(customVectorRetriever, keywordRetriever, documentJoiner, directExecutor);

        ScopedValue.where(
                        FilterContext.FILTER_EXPRESSION,
                        new FilterExpressionBuilder().eq("test", "value").build())
                .run(() -> {
                    retriever.retrieve(new Query("test"));
                    assertThat(vectorFilter.get().toString()).contains("test");
                });
    }
}
