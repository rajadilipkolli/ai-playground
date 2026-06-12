package com.learning.ai.service;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class StructureAwareChunker {

    private final DocumentSplitter textSplitter;
    private final int maxChunkSize;

    public StructureAwareChunker(
            @Value("${langchain4j.rag.chunking.size:300}") int chunkSize,
            @Value("${langchain4j.rag.chunking.overlap:50}") int overlap) {
        this.maxChunkSize = chunkSize;
        this.textSplitter = DocumentSplitters.recursive(chunkSize, overlap);
    }

    public List<TextSegment> chunk(Document document) {
        // Ideally, Docling returns structured elements (e.g. paragraphs, tables) as separated blocks.
        // For a generic Document containing plain text or markdown, we'll implement a custom logic
        // that looks for table boundaries (e.g. markdown tables like |---|---|).
        // Since we don't have the rich Docling structure in the raw Document yet, we'll approximate it.
        // We'll split the document into logical sections (e.g. separated by double newlines) and then process.
        
        List<TextSegment> finalSegments = new ArrayList<>();
        String content = document.text();
        
        // Simple heuristic: split by double newlines to isolate tables or paragraphs
        String[] blocks = content.split("\n\n");
        
        for (String block : blocks) {
            block = block.trim();
            if (block.isEmpty()) continue;
            
            if (isMarkdownTable(block)) {
                if (block.length() <= maxChunkSize) {
                    finalSegments.add(TextSegment.from(block, document.metadata().copy().put("element_type", "table")));
                } else {
                    finalSegments.addAll(splitLargeTable(block, document.metadata()));
                }
            } else {
                // Regular text section
                Document sectionDoc = Document.from(block, document.metadata().copy().put("element_type", "text"));
                finalSegments.addAll(textSplitter.split(sectionDoc));
            }
        }
        
        return finalSegments;
    }
    
    private boolean isMarkdownTable(String block) {
        return block.contains("|") && block.contains("\n|") && block.contains("---");
    }

    private List<TextSegment> splitLargeTable(String tableContent, dev.langchain4j.data.document.Metadata metadata) {
        List<TextSegment> chunks = new ArrayList<>();
        String[] lines = tableContent.split("\n");
        
        // Extract header
        StringBuilder header = new StringBuilder();
        int dataStartIndex = 0;
        for (int i = 0; i < lines.length; i++) {
            header.append(lines[i]).append("\n");
            if (lines[i].contains("---")) {
                dataStartIndex = i + 1;
                break;
            }
        }
        
        StringBuilder currentChunk = new StringBuilder(header);
        for (int i = dataStartIndex; i < lines.length; i++) {
            if (currentChunk.length() + lines[i].length() > maxChunkSize && currentChunk.length() > header.length()) {
                chunks.add(TextSegment.from(currentChunk.toString().trim(), metadata.copy().put("element_type", "table")));
                currentChunk = new StringBuilder(header);
            }
            currentChunk.append(lines[i]).append("\n");
        }
        
        if (currentChunk.length() > header.length()) {
            chunks.add(TextSegment.from(currentChunk.toString().trim(), metadata.copy().put("element_type", "table")));
        }
        
        return chunks;
    }
}
