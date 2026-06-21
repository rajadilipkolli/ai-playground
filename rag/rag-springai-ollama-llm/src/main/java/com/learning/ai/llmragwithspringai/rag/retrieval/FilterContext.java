package com.learning.ai.llmragwithspringai.rag.retrieval;

import org.springframework.ai.vectorstore.filter.Filter;

public class FilterContext {
    public static final ScopedValue<Filter.Expression> FILTER_EXPRESSION = ScopedValue.newInstance();

    public static Filter.Expression getFilterExpression() {
        return FILTER_EXPRESSION.isBound() ? FILTER_EXPRESSION.get() : null;
    }
}
