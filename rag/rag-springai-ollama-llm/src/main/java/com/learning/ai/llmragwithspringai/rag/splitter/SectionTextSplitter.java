package com.learning.ai.llmragwithspringai.rag.splitter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.util.StringUtils;

public class SectionTextSplitter extends TextSplitter {

    private final Pattern sectionPattern;
    private final TokenTextSplitter fallbackSplitter;

    public SectionTextSplitter(String pattern, TokenTextSplitter fallbackSplitter) {
        this.sectionPattern = Pattern.compile(pattern, Pattern.MULTILINE);
        this.fallbackSplitter = fallbackSplitter;
    }

    @Override
    protected List<String> splitText(String text) {
        if (!StringUtils.hasText(text)) {
            return List.of();
        }
        List<String> chunks = new ArrayList<>();
        Matcher matcher = sectionPattern.matcher(text);
        int lastEnd = 0;
        while (matcher.find()) {
            int start = matcher.start();
            if (start > lastEnd) {
                String chunk = text.substring(lastEnd, start).trim();
                if (!chunk.isEmpty()) {
                    List<Document> subChunks = fallbackSplitter.apply(List.of(new Document(chunk)));
                    for (Document subChunk : subChunks) {
                        chunks.add(subChunk.getText());
                    }
                }
            }
            lastEnd = matcher.end();
        }
        if (lastEnd < text.length()) {
            String chunk = text.substring(lastEnd).trim();
            if (!chunk.isEmpty()) {
                List<Document> subChunks = fallbackSplitter.apply(List.of(new Document(chunk)));
                for (Document subChunk : subChunks) {
                    chunks.add(subChunk.getText());
                }
            }
        }
        return chunks;
    }

    @Override
    public List<Document> apply(List<Document> documents) {
        List<Document> result = new ArrayList<>();

        for (Document document : documents) {
            String text = document.getText();
            if (text == null || text.isEmpty()) {
                continue;
            }

            Matcher matcher = sectionPattern.matcher(text);
            int lastEnd = 0;
            String currentSectionTitle = "Document Start";

            while (matcher.find()) {
                int start = matcher.start();
                int end = matcher.end();

                if (start > lastEnd) {
                    String chunkText = text.substring(lastEnd, start).trim();
                    if (!chunkText.isEmpty()) {
                        processChunk(chunkText, currentSectionTitle, document, result);
                    }
                }

                currentSectionTitle = matcher.group().trim();
                lastEnd = end;
            }

            if (lastEnd < text.length()) {
                String chunkText = text.substring(lastEnd).trim();
                if (!chunkText.isEmpty()) {
                    processChunk(chunkText, currentSectionTitle, document, result);
                }
            }
        }

        return result;
    }

    private void processChunk(String chunkText, String sectionTitle, Document originalDoc, List<Document> result) {
        Map<String, Object> chunkMetadata = new HashMap<>(originalDoc.getMetadata());
        chunkMetadata.put("section_title", sectionTitle);

        Document chunkDoc = Document.builder()
                .id(UUID.randomUUID().toString())
                .text(chunkText)
                .metadata(chunkMetadata)
                .build();

        List<Document> subChunks = fallbackSplitter.apply(List.of(chunkDoc));
        result.addAll(subChunks);
    }
}
