package com.learning.ai.llmragwithspringai.rag.retrieval;

import java.util.List;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.filter.Filter;

public class FilterContext {
    public static final ScopedValue<Filter.Expression> FILTER_EXPRESSION = ScopedValue.newInstance();

    public static Filter.Expression getFilterExpression() {
        return FILTER_EXPRESSION.isBound() ? FILTER_EXPRESSION.get() : null;
    }

    private static final ThreadLocal<List<Document>> RETRIEVED_DOCUMENTS = new ThreadLocal<>();

    public static void setRetrievedDocuments(List<Document> docs) {
        RETRIEVED_DOCUMENTS.set(docs);
    }

    public static List<Document> getRetrievedDocuments() {
        return RETRIEVED_DOCUMENTS.get();
    }

    public static void clearRetrievedDocuments() {
        RETRIEVED_DOCUMENTS.remove();
    }
}
