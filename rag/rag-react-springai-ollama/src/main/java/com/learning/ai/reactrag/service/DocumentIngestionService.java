package com.learning.ai.reactrag.service;

import com.learning.ai.reactrag.util.ContentHashUtil;
import io.micrometer.observation.annotation.Observed;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DocumentIngestionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentIngestionService.class);

    private final VectorStore vectorStore;
    private final TokenTextSplitter tokenTextSplitter;

    public DocumentIngestionService(VectorStore vectorStore, TokenTextSplitter tokenTextSplitter) {
        this.vectorStore = vectorStore;
        this.tokenTextSplitter = tokenTextSplitter;
    }

    @Observed(name = "document.ingestion", contextualName = "document-ingestion")
    public int ingestFile(MultipartFile file) {
        String filename = file.getOriginalFilename();
        LOGGER.info("Starting ingestion for file: {}", filename);

        Resource resource = file.getResource();
        ContentHashUtil.HashResult hashResult = ContentHashUtil.calculateHash(resource);
        String hash = hashResult.hash();
        Resource rereadableResource = hashResult.rereadableResource();

        List<Document> documents;
        if (filename != null && filename.toLowerCase().endsWith(".pdf")) {
            PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(
                    rereadableResource,
                    PdfDocumentReaderConfig.builder()
                            .withPageExtractedTextFormatter(ExtractedTextFormatter.builder()
                                    .withNumberOfBottomTextLinesToDelete(0)
                                    .withNumberOfTopPagesToSkipBeforeDelete(0)
                                    .build())
                            .withPagesPerDocument(1)
                            .build());
            documents = pdfReader.get();
        } else {
            TextReader textReader = new TextReader(rereadableResource);
            textReader.getCustomMetadata().put("filename", filename);
            documents = textReader.get();
        }

        LOGGER.info("Read {} documents from file. Enhancing metadata...", documents.size());

        for (Document document : documents) {
            Map<String, Object> metadata = document.getMetadata();
            metadata.put("source", filename);
            metadata.put("contentHash", hash);
            metadata.put("ingestionTimestamp", Instant.now().toString());
        }

        List<Document> splitDocuments = tokenTextSplitter.apply(documents);

        List<Document> existing = vectorStore.similaritySearch(SearchRequest.builder()
                .query("")
                .filterExpression("contentHash == '" + hash + "'")
                .topK(1)
                .build());

        if (!existing.isEmpty()) {
            LOGGER.info("Document with hash {} already exists. Skipping vector store insertion.", hash);
            return 0;
        }

        LOGGER.info("Split into {} chunks. Saving to vector store...", splitDocuments.size());

        vectorStore.accept(splitDocuments);

        LOGGER.info("Ingestion completed for file: {}", filename);
        return splitDocuments.size();
    }
}
