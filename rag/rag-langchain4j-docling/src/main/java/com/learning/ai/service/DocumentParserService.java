package com.learning.ai.service;

import com.learning.ai.parser.DoclingDocumentParser;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import java.io.InputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class DocumentParserService {

    private static final Logger log = LoggerFactory.getLogger(DocumentParserService.class);

    private final DocumentParser primaryParser;
    private final DocumentParser fallbackParser;

    public DocumentParserService(@Value("${docling.server.url}") String doclingServerUrl) {
        this.primaryParser = new DoclingDocumentParser(doclingServerUrl);
        this.fallbackParser = new ApachePdfBoxDocumentParser();
    }

    public Document parse(InputStream inputStream) {
        try {
            log.info("Attempting to parse document using DoclingDocumentParser");
            // InputStream is consumed here, ideally we should clone it or use mark/reset if fallback is needed.
            // Since InputStream from web usually doesn't support mark, we might need to buffer it.
            // For now, assuming caller passes a repeatable stream or byte array input stream.
            return primaryParser.parse(inputStream);
        } catch (Exception e) {
            log.error("Failed to parse document with Docling: {}. Falling back to ApachePdfBoxDocumentParser.", e.getMessage(), e);
            try {
                // Warning: If inputStream was partially consumed, this will fail unless it's a ByteArrayInputStream.
                // In a robust implementation, the caller should provide a ByteArrayInputStream or similar.
                return fallbackParser.parse(inputStream);
            } catch (Exception ex) {
                log.error("Fallback ApachePdfBoxDocumentParser also failed: {}", ex.getMessage(), ex);
                throw new RuntimeException("Failed to parse document using both primary and fallback parsers.", ex);
            }
        }
    }
}
