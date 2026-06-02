package com.learning.ai.llmragwithspringai.service;

import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

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

    public void loadData(Resource documentResource) {
        if (!isEmpty()) {
            LOGGER.info("Vector store is not empty. Skipping ingestion.");
            return;
        }

        long startTime = System.currentTimeMillis();
        DocumentReader documentReader = null;
        if (documentResource.getFilename() != null
                && documentResource.getFilename().endsWith(".pdf")) {
            LOGGER.info("Loading PDF document");
            PdfDocumentReaderConfig pdfDocumentReaderConfig = PdfDocumentReaderConfig.builder()
                    .withPageExtractedTextFormatter(ExtractedTextFormatter.builder()
                            .withNumberOfBottomTextLinesToDelete(3)
                            .withNumberOfTopPagesToSkipBeforeDelete(1)
                            .build())
                    .withPagesPerDocument(1)
                    .build();
            documentReader = new PagePdfDocumentReader(documentResource, pdfDocumentReaderConfig);
        } else if (documentResource.getFilename() != null
                && documentResource.getFilename().endsWith(".txt")) {
            documentReader = new TextReader(documentResource);
        } else if (documentResource.getFilename() != null
                && documentResource.getFilename().endsWith(".json")) {
            documentReader = new JsonReader(documentResource);
        }
        if (documentReader != null) {
            LOGGER.info("Loading text document to vector database");
            DocumentTransformer metadataEnricher = documents -> {
                documents.forEach(d -> {
                    Map<String, Object> metadata = d.getMetadata();
                    metadata.put("EXTERNAL_KNOWLEDGE", "true");
                });
                return documents;
            };

            List<Document> docsToIngest = metadataEnricher.apply(tokenTextSplitter.apply(documentReader.get()));
            vectorStore.accept(docsToIngest);

            long duration = System.currentTimeMillis() - startTime;
            LOGGER.info("Loaded {} chunks to vector database in {} ms.", docsToIngest.size(), duration);
            meterRegistry.timer("rag.ingestion.latency").record(Duration.ofMillis(duration));
            meterRegistry.counter("rag.documents.ingested").increment(docsToIngest.size());
        }
    }

    public long count() {
        return Objects.requireNonNull(this.vectorStore.similaritySearch("*")).size();
    }

    public boolean isEmpty() {
        return count() == 0;
    }
}
