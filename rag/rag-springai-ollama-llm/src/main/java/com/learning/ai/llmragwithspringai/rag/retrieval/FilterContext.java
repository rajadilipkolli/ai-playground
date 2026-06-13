package com.learning.ai.llmragwithspringai.rag.retrieval;

public class FilterContext {
    public static final ScopedValue<String> FILTER_EXPRESSION = ScopedValue.newInstance();

    public static String getFilterExpression() {
        return FILTER_EXPRESSION.isBound() ? FILTER_EXPRESSION.get() : null;
    }
}
