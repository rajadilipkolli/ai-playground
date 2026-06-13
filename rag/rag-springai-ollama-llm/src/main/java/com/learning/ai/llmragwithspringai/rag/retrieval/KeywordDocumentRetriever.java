package com.learning.ai.llmragwithspringai.rag.retrieval;

import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionTextParser;
import org.springframework.ai.vectorstore.pgvector.PgVectorFilterExpressionConverter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

public class KeywordDocumentRetriever implements DocumentRetriever {

    private static final Logger log = LoggerFactory.getLogger(KeywordDocumentRetriever.class);
    private static final TypeReference<Map<String, Object>> MAP_TYPE_REF = new TypeReference<>() {};

    private final JdbcTemplate jdbcTemplate;
    private final int topK;
    private final JsonMapper jsonMapper;

    public KeywordDocumentRetriever(JdbcTemplate jdbcTemplate, int topK, JsonMapper jsonMapper) {
        if (topK <= 0) {
            throw new IllegalArgumentException("topK must be > 0");
        }
        this.jdbcTemplate = jdbcTemplate;
        this.topK = topK;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public List<Document> retrieve(Query query) {
        String filterString = FilterContext.getFilterExpression();
        String metadataFilterSql = "";
        if (filterString != null && !filterString.isBlank()) {
            FilterExpressionTextParser parser = new FilterExpressionTextParser();
            Filter.Expression filter = parser.parse(filterString);
            PgVectorFilterExpressionConverter converter = new PgVectorFilterExpressionConverter();
            metadataFilterSql = " AND " + converter.convertExpression(filter);
        }

        String sql = "SELECT id, content, metadata, ts_rank(content_tsv, plainto_tsquery('english', ?)) as rank "
                + "FROM vector_store "
                + "WHERE content_tsv @@ plainto_tsquery('english', ?) "
                + metadataFilterSql
                + " " + "ORDER BY rank DESC "
                + "LIMIT ?";

        String text = query.text();
        log.debug("Executing keyword search for query: {}, topK: {}, filter: {}", text, topK, filterString);

        return jdbcTemplate.query(sql, documentRowMapper(), text, text, topK);
    }

    private RowMapper<Document> documentRowMapper() {
        return (rs, rowNum) -> {
            String id = rs.getString("id");
            String content = rs.getString("content");
            String metadataJson = rs.getString("metadata");
            float rank = rs.getFloat("rank");

            Map<String, Object> metadataMap = null;
            if (metadataJson != null && !metadataJson.isBlank()) {
                try {
                    metadataMap = jsonMapper.readValue(metadataJson, MAP_TYPE_REF);
                } catch (JacksonException e) {
                    log.warn("Failed to parse metadata JSON for document id: {}", id, e);
                }
            }

            Document.Builder documentBuilder = Document.builder().id(id).text(content);

            if (metadataMap != null) {
                documentBuilder.metadata(metadataMap);
            }

            // Add the keyword rank to metadata
            documentBuilder.metadata("ts_rank", rank);

            return documentBuilder.build();
        };
    }
}
