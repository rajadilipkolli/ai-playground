package com.learning.ai.llmragwithspringai.service;

import com.learning.ai.llmragwithspringai.model.response.IngestionResult;
import com.learning.ai.llmragwithspringai.model.response.IngestionStatus;
import com.learning.ai.llmragwithspringai.util.ContentHashUtil;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.annotation.Observed;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.document.DocumentTransformer;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.JsonReader;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StopWatch;

@Service
public class DataIndexerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataIndexerService.class);

    private final TextSplitter tokenTextSplitter;
    private final VectorStore vectorStore;
    private final MeterRegistry meterRegistry;
    private final JdbcTemplate jdbcTemplate;

    public DataIndexerService(
            TextSplitter tokenTextSplitter,
            VectorStore vectorStore,
            MeterRegistry meterRegistry,
            JdbcTemplate jdbcTemplate) {
        this.tokenTextSplitter = tokenTextSplitter;
        this.vectorStore = vectorStore;
        this.meterRegistry = meterRegistry;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Observed(name = "rag.ingest", contextualName = "rag-ingest")
    @Transactional
    public IngestionResult loadData(Resource documentResource, String documentType, String owner, String category) {
        String filename = documentResource.getFilename();
        if (filename == null) {
            filename = "unknown";
        }

        StopWatch stopWatch = new StopWatch("loadData");
        stopWatch.start();
        ContentHashUtil.HashResult hashResult = ContentHashUtil.calculateHash(documentResource);
        String contentHash = hashResult.hash();
        final Resource rereadableResource = hashResult.rereadableResource();

        List<String> existingByHash = findDocumentsByContentHash(contentHash);
        if (!existingByHash.isEmpty()) {
            LOGGER.info("Document {} with hash {} already exists. Skipping ingestion.", filename, contentHash);
            return new IngestionResult(IngestionStatus.SKIPPED_DUPLICATE, filename, 0, 0);
        }

        List<String> existingByFilename = findDocumentsByFilename(filename, documentType, owner, category);
        int chunksDeleted = 0;
        if (!existingByFilename.isEmpty()) {
            LOGGER.info(
                    "Document {} exists with different hash for scope documentType='{}', owner='{}', category='{}'. Replacing {} old chunks.",
                    filename,
                    documentType,
                    owner,
                    category,
                    existingByFilename.size());
            vectorStore.delete(existingByFilename);
            chunksDeleted = existingByFilename.size();
        }

        String ingestedAt = DateTimeFormatter.ISO_INSTANT.format(Instant.now());

        DocumentReader documentReader = null;
        if (filename.endsWith(".pdf")) {
            LOGGER.info("Loading PDF document");
            PdfDocumentReaderConfig pdfDocumentReaderConfig = PdfDocumentReaderConfig.builder()
                    .withPageExtractedTextFormatter(ExtractedTextFormatter.builder()
                            .withNumberOfBottomTextLinesToDelete(3)
                            .withNumberOfTopPagesToSkipBeforeDelete(1)
                            .build())
                    .withPagesPerDocument(1)
                    .build();
            documentReader = new PagePdfDocumentReader(rereadableResource, pdfDocumentReaderConfig);
        } else if (filename.endsWith(".txt")) {
            documentReader = new TextReader(rereadableResource);
        } else if (filename.endsWith(".json")) {
            documentReader = new JsonReader(rereadableResource);
        }

        if (documentReader != null) {
            LOGGER.info("Loading text document to vector database");
            DocumentTransformer metadataEnricher = documents -> {
                final String finalFilename =
                        rereadableResource.getFilename() != null ? rereadableResource.getFilename() : "unknown";
                documents.forEach(d -> {
                    Map<String, Object> metadata = d.getMetadata();
                    metadata.put("EXTERNAL_KNOWLEDGE", "true");
                    metadata.put("source_filename", finalFilename);
                    metadata.put("content_hash", contentHash);
                    metadata.put("ingested_at", ingestedAt);
                    if (documentType != null) metadata.put("documentType", documentType);
                    if (owner != null) metadata.put("owner", owner);
                    if (category != null) metadata.put("category", category);
                });
                return documents;
            };

            List<Document> docsToIngest = metadataEnricher.apply(tokenTextSplitter.apply(documentReader.get()));
            vectorStore.accept(docsToIngest);

            stopWatch.stop();
            LOGGER.info(
                    "Loaded {} chunks to vector database in {} ms.",
                    docsToIngest.size(),
                    stopWatch.getTotalTimeMillis());
            meterRegistry.timer("rag.ingestion.latency").record(Duration.ofMillis(stopWatch.getTotalTimeMillis()));
            meterRegistry.counter("rag.documents.ingested").increment(docsToIngest.size());

            IngestionStatus status = chunksDeleted > 0 ? IngestionStatus.REPLACED : IngestionStatus.INGESTED;
            return new IngestionResult(status, filename, docsToIngest.size(), chunksDeleted);
        }

        return new IngestionResult(IngestionStatus.SKIPPED_DUPLICATE, filename, 0, 0); // fallback
    }

    private List<String> findDocumentsByContentHash(String hash) {
        return jdbcTemplate.queryForList(
                "SELECT id FROM vector_store WHERE metadata->>'content_hash' = ?", String.class, hash);
    }

    private List<String> findDocumentsByFilename(String filename, String documentType, String owner, String category) {
        String sql = "SELECT id FROM vector_store WHERE metadata->>'source_filename' = ?";
        if (documentType != null) {
            sql += " AND metadata->>'documentType' = ?";
        }
        if (owner != null) {
            sql += " AND metadata->>'owner' = ?";
        }
        if (category != null) {
            sql += " AND metadata->>'category' = ?";
        }

        var args = new ArrayList<Object>();
        args.add(filename);
        if (documentType != null) {
            args.add(documentType);
        }
        if (owner != null) {
            args.add(owner);
        }
        if (category != null) {
            args.add(category);
        }

        return jdbcTemplate.queryForList(sql, String.class, args.toArray());
    }

    @Observed(name = "rag.count", contextualName = "rag-count")
    public long count() {
        Long count = this.jdbcTemplate.queryForObject("SELECT COUNT(1) FROM vector_store", Long.class);
        return count != null ? count : 0L;
    }

    public boolean isEmpty() {
        return count() == 0;
    }
}
