package com.learning.ai.llmragwithspringai.rag.retrieval;

public class FilterContext {
    private static final ThreadLocal<String> FILTER_EXPRESSION = new ThreadLocal<>();

    public static void setFilterExpression(String expression) {
        FILTER_EXPRESSION.set(expression);
    }

    public static String getFilterExpression() {
        return FILTER_EXPRESSION.get();
    }

    public static void clear() {
        FILTER_EXPRESSION.remove();
    }
}
