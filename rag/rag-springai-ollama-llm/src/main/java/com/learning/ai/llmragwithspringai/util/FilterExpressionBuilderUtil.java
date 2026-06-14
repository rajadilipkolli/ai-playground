package com.learning.ai.llmragwithspringai.util;

import java.util.Collection;
import java.util.Map;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;

public class FilterExpressionBuilderUtil {

    /**
     * Builds a filter expression from a map of filter key/value pairs.
     * Values may be plain strings or lists (as returned by some LLMs); in the
     * latter case the first element is used.
     */
    public static Filter.Expression build(Map<String, Object> filters) {
        if (filters == null || filters.isEmpty()) {
            return null;
        }

        FilterExpressionBuilder b = new FilterExpressionBuilder();
        FilterExpressionBuilder.Op expression = null;

        for (Map.Entry<String, Object> entry : filters.entrySet()) {
            String value = toStringValue(entry.getValue());
            if (value == null || value.isBlank()) {
                continue;
            }
            FilterExpressionBuilder.Op eq = b.eq(entry.getKey(), value);
            if (expression == null) {
                expression = eq;
            } else {
                expression = b.and(expression, eq);
            }
        }

        return expression == null ? null : expression.build();
    }

    /** Converts a filter value to a String, unwrapping single-element collections. */
    private static String toStringValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Collection<?> col) {
            return col.isEmpty() ? null : col.iterator().next().toString();
        }
        return value.toString();
    }
}
