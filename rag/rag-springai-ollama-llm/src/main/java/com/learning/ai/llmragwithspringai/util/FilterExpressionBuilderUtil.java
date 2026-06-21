package com.learning.ai.llmragwithspringai.util;

import java.util.Collection;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;

public class FilterExpressionBuilderUtil {

    private static final Pattern VALID_KEY_PATTERN = Pattern.compile("^\\w+$");

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
            String key = entry.getKey();
            if (key == null || key.isBlank() || !VALID_KEY_PATTERN.matcher(key).matches()) {
                continue;
            }
            Object value = unwrapValue(entry.getValue());
            if (value == null || (value instanceof String str && str.isBlank())) {
                continue;
            }
            FilterExpressionBuilder.Op eq = b.eq(key, value);
            if (expression == null) {
                expression = eq;
            } else {
                expression = b.and(expression, eq);
            }
        }

        return expression == null ? null : expression.build();
    }

    /** Unwraps single-element collections, preserving original types (e.g. Number, Boolean). */
    private static Object unwrapValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Collection<?> col) {
            if (col.isEmpty()) {
                return null;
            }
            return col.iterator().next();
        }
        return value;
    }
}
