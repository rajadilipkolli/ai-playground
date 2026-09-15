package com.learning.ai.service;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DocumentParserService {

    private static final Logger log = LoggerFactory.getLogger(DocumentParserService.class);

    private final DocumentParser primaryParser;
    private final DocumentParser fallbackParser;

    /** Creates a Docling parser with a PDFBox fallback. */
    public DocumentParserService(DocumentParser doclingDocumentParser) {
        this.primaryParser = doclingDocumentParser;
        this.fallbackParser = new ApachePdfBoxDocumentParser();
    }

    /**
     * Parses a buffered copy of the stream with the primary parser, retrying the complete content with
     * the fallback parser if the primary parser fails.
     *
     * @throws RuntimeException if the stream cannot be read or both parsers fail
     */
    public Document parse(InputStream inputStream) {
        final byte[] documentBytes;
        try {
            documentBytes = inputStream.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException("Failed to read document input stream.", e);
        }

        try (InputStream primaryInput = new ByteArrayInputStream(documentBytes)) {
            log.info("Attempting to parse document using DoclingDocumentParser");
            return primaryParser.parse(primaryInput);
        } catch (Exception e) {
            log.error(
                    "Failed to parse document with Docling: {}. Falling back to ApachePdfBoxDocumentParser.",
                    e.getMessage(),
                    e);
            try (InputStream fallbackInput = new ByteArrayInputStream(documentBytes)) {
                return fallbackParser.parse(fallbackInput);
            } catch (Exception ex) {
                log.error("Fallback ApachePdfBoxDocumentParser also failed: {}", ex.getMessage(), ex);
                throw new RuntimeException("Failed to parse document using both primary and fallback parsers.", ex);
            }
        }
    }
}
