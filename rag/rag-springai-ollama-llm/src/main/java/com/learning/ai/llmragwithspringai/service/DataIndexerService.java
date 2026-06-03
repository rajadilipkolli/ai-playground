package com.learning.ai.llmragwithspringai.service;

import com.learning.ai.llmragwithspringai.model.response.IngestionResult;
import com.learning.ai.llmragwithspringai.util.ContentHashUtil;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
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
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

@Service
public class DataIndexerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataIndexerService.class);

    private final TokenTextSplitter tokenTextSplitter;
    private final VectorStore vectorStore;
    private final MeterRegistry meterRegistry;

    public DataIndexerService(
            TokenTextSplitter tokenTextSplitter, VectorStore vectorStore, MeterRegistry meterRegistry) {
        this.tokenTextSplitter = tokenTextSplitter;
        this.vectorStore = vectorStore;
        this.meterRegistry = meterRegistry;
    }

    public IngestionResult loadData(Resource documentResource) {
        String filename = documentResource.getFilename();
        if (filename == null) {
            filename = "unknown";
        }

        StopWatch stopWatch = new StopWatch("loadData");
        stopWatch.start();
        String contentHash = ContentHashUtil.calculateHash(documentResource);

        List<String> existingByHash = findDocumentsByContentHash(contentHash);
        if (!existingByHash.isEmpty()) {
            LOGGER.info("Document {} with hash {} already exists. Skipping ingestion.", filename, contentHash);
            return new IngestionResult("skipped_duplicate", filename, 0, 0);
        }

        List<String> existingByFilename = findDocumentsByFilename(filename);
        int chunksDeleted = 0;
        if (!existingByFilename.isEmpty()) {
            LOGGER.info(
                    "Document {} exists with different hash. Replacing {} old chunks.",
                    filename,
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
            documentReader = new PagePdfDocumentReader(documentResource, pdfDocumentReaderConfig);
        } else if (filename.endsWith(".txt")) {
            documentReader = new TextReader(documentResource);
        } else if (filename.endsWith(".json")) {
            documentReader = new JsonReader(documentResource);
        }

        if (documentReader != null) {
            LOGGER.info("Loading text document to vector database");
            DocumentTransformer metadataEnricher = documents -> {
                final String finalFilename =
                        documentResource.getFilename() != null ? documentResource.getFilename() : "unknown";
                documents.forEach(d -> {
                    Map<String, Object> metadata = d.getMetadata();
                    metadata.put("EXTERNAL_KNOWLEDGE", "true");
                    metadata.put("source_filename", finalFilename);
                    metadata.put("content_hash", contentHash);
                    metadata.put("ingested_at", ingestedAt);
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

            String status = chunksDeleted > 0 ? "replaced" : "ingested";
            return new IngestionResult(status, filename, docsToIngest.size(), chunksDeleted);
        }

        return new IngestionResult("skipped_duplicate", filename, 0, 0); // fallback
    }

    private List<String> findDocumentsByContentHash(String hash) {
        SearchRequest searchRequest = SearchRequest.builder()
                .filterExpression("content_hash == '" + hash + "'")
                .topK(10000)
                .build();
        return vectorStore.similaritySearch(searchRequest).stream()
                .map(Document::getId)
                .collect(Collectors.toList());
    }

    private List<String> findDocumentsByFilename(String filename) {
        SearchRequest searchRequest = SearchRequest.builder()
                .filterExpression("source_filename == '" + filename + "'")
                .topK(10000)
                .build();
        return vectorStore.similaritySearch(searchRequest).stream()
                .map(Document::getId)
                .collect(Collectors.toList());
    }

    public long count() {
        return Objects.requireNonNull(this.vectorStore.similaritySearch("*")).size();
    }

    public boolean isEmpty() {
        return count() == 0;
    }
}
