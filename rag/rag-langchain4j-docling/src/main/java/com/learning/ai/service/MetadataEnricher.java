package com.learning.ai.service;

import dev.langchain4j.data.segment.TextSegment;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class MetadataEnricher {

    public List<TextSegment> enrich(List<TextSegment> segments, String sourceFilename, String contentHash) {
        String documentId = UUID.randomUUID().toString();
        String ingestedAt = ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);
        
        String currentSectionPath = "root";
        int tableCounter = 0;
        
        for (TextSegment segment : segments) {
            dev.langchain4j.data.document.Metadata metadata = segment.metadata();
            
            // Set provenance
            metadata.put("document_id", documentId);
            metadata.put("source_filename", sourceFilename);
            metadata.put("content_hash", contentHash);
            metadata.put("ingested_at", ingestedAt);
            
            String text = segment.text();
            
            // Heuristic for heading tracking (Assuming markdown output from Docling)
            if (text.startsWith("#")) {
                int headingLevel = 0;
                while (headingLevel < text.length() && text.charAt(headingLevel) == '#') {
                    headingLevel++;
                }
                metadata.put("heading_level", String.valueOf(headingLevel));
                
                // Very basic section path logic
                String headingText = text.replace("#", "").trim().split("\n")[0];
                if (headingText.length() > 50) {
                    headingText = headingText.substring(0, 50);
                }
                currentSectionPath = currentSectionPath + "/" + headingText.replaceAll("[^a-zA-Z0-9_-]", "");
                metadata.put("element_type", "heading");
            } else if (metadata.getString("element_type") == null) {
                metadata.put("element_type", "text");
            }
            
            if ("table".equals(metadata.getString("element_type"))) {
                if (!metadata.containsKey("table_id")) {
                    metadata.put("table_id", "tbl_" + (++tableCounter));
                }
            }
            
            metadata.put("section_path", currentSectionPath);
            
            // Try to extract page number if Docling left a tag, otherwise leave it absent
            // Example tag Docling might add: <!-- Page 1 -->
            if (text.contains("<!-- Page ")) {
                try {
                    int start = text.indexOf("<!-- Page ") + 10;
                    int end = text.indexOf(" -->", start);
                    String pageNumStr = text.substring(start, end).trim();
                    metadata.put("page_number", pageNumStr);
                } catch (Exception ignored) {
                }
            }
        }
        
        return segments;
    }
}
