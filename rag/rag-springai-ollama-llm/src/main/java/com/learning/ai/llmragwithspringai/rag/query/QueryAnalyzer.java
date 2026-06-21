package com.learning.ai.llmragwithspringai.rag.query;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import tools.jackson.databind.json.JsonMapper;

public class QueryAnalyzer {

    private static final Logger LOGGER = LoggerFactory.getLogger(QueryAnalyzer.class);

    // Matches: "someKey": "someValue"  or  "someKey": 2023
    private static final Pattern KV_PATTERN =
            Pattern.compile("\"(\\w+)\"\\s*:\\s*(?:\"([^\"]*)\"|(-?\\d+(?:\\.\\d+)?))");

    private final ChatClient chatClient;
    private final JsonMapper jsonMapper;

    public QueryAnalyzer(ChatClient.Builder chatClientBuilder, JsonMapper jsonMapper) {
        this.chatClient = chatClientBuilder.build();
        this.jsonMapper = jsonMapper;
    }

    public QueryAnalysisResult analyze(String query) {
        String prompt = """
                You are an AI assistant that extracts metadata filters from a user query.
                Identify any temporal references (e.g. year), categories, and document types.
                Remove the filter terms from the query and return the cleaned query.
                Valid filter keys are: 'category', 'documentType', 'owner', 'year'.

                You MUST respond with ONLY a JSON object that has exactly these two fields:
                  "cleanedQuery": the original query with the filter terms removed (must not be empty)
                  "filters": a flat object of extracted filter key/value pairs

                Example input : "Show me all HR policies from 2023"
                Example output: {"cleanedQuery":"Show me all policies","filters":{"category":"HR","year":"2023"}}

                No markdown, no code fences, no extra text. Only the raw JSON object.
                """;

        String rawResponse;
        try {
            rawResponse = chatClient.prompt().system(prompt).user(query).call().content();
        } catch (Exception e) {
            LOGGER.warn("Query analysis failed, falling back to original query: {}", e.getMessage());
            return new QueryAnalysisResult(query, Map.of());
        }
        LOGGER.debug("Raw QueryAnalyzer response: {}", rawResponse);
        return parse(rawResponse, query);
    }

    /**
     * Attempts to parse the LLM response into a {@link QueryAnalysisResult}.
     * <ol>
     *   <li>Stage 1 – strip markdown fences, extract the outermost JSON object and parse normally.</li>
     *   <li>Stage 2 – if the JSON is malformed, fall back to regex extraction of every
     *       {@code "key": "value"} / {@code "key": number} pair found anywhere in the text.</li>
     * </ol>
     * If {@code cleanedQuery} cannot be determined it defaults to the original query.
     */
    QueryAnalysisResult parse(String raw, String originalQuery) {
        if (raw == null || raw.isBlank()) {
            return new QueryAnalysisResult(originalQuery, Map.of());
        }

        // Remove markdown fences (```json … ``` or ``` … ```)
        String cleaned = raw.replaceAll("(?s)```(?:json)?\\s*", "").trim();

        // Stage 1: try standard JSON parse on the first complete {...} block
        String jsonBlock = extractJsonBlock(cleaned);
        if (jsonBlock != null) {
            try {
                QueryAnalysisResult result = jsonMapper.readValue(jsonBlock, QueryAnalysisResult.class);
                return withFallbackQuery(result, originalQuery);
            } catch (Exception e) {
                LOGGER.debug("JSON parse failed ({}), switching to regex extraction", e.getMessage());
            }
        }

        // Stage 2: regex extraction – collect all key/value pairs from the whole text
        String cleanedQuery = null;
        Map<String, Object> filters = new LinkedHashMap<>();

        Matcher m = KV_PATTERN.matcher(cleaned);
        while (m.find()) {
            String key = m.group(1);
            String strVal = m.group(2); // quoted value, may be null
            String numVal = m.group(3); // numeric value, may be null
            Object value = strVal != null ? strVal : numVal;

            if ("cleanedQuery".equals(key)) {
                cleanedQuery = (String) value;
            } else if (!"filters".equals(key)) {
                // everything else is treated as a filter key
                filters.put(key, value);
            }
        }

        if (cleanedQuery == null || cleanedQuery.isBlank()) {
            cleanedQuery = originalQuery;
        }
        return new QueryAnalysisResult(cleanedQuery, filters);
    }

    /** Returns the substring between the first '{' and the matching last '}', or {@code null}. */
    private @Nullable String extractJsonBlock(String text) {
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start != -1 && end > start) {
            return text.substring(start, end + 1);
        }
        return null;
    }

    private QueryAnalysisResult withFallbackQuery(@Nullable QueryAnalysisResult result, String originalQuery) {
        if (result == null) {
            return new QueryAnalysisResult(originalQuery, Map.of());
        }
        String cleaned = result.cleanedQuery().isBlank() ? originalQuery : result.cleanedQuery();
        Map<String, Object> filters = result.filters() != null ? result.filters() : Map.of();
        return new QueryAnalysisResult(cleaned, filters);
    }
}
