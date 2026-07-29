package com.learning.ai.llmragwithspringai.rag.splitter;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;

class SectionTextSplitterTest {

    private final TokenTextSplitter fallback =
            TokenTextSplitter.builder().withChunkSize(100).build();
    private final SectionTextSplitter splitter = new SectionTextSplitter("(^#+\\s+.*$)|(\\n\\n)", fallback);

    @Test
    void shouldSplitByMarkdownHeader() {
        String text = "# Header 1\nContent 1\n## Header 2\nContent 2";
        List<Document> docs = splitter.apply(List.of(new Document(text)));

        assertThat(docs).hasSize(2);
        assertThat(docs.get(0).getText()).isEqualTo("Content 1");
        assertThat(docs.get(1).getText()).isEqualTo("Content 2");
        assertThat(docs.get(0).getMetadata()).containsEntry("section_title", "# Header 1");
        assertThat(docs.get(1).getMetadata()).containsEntry("section_title", "## Header 2");
    }

    @Test
    void shouldSplitByDoubleNewline() {
        String text = "Paragraph 1\n\nParagraph 2";
        List<Document> docs = splitter.apply(List.of(new Document(text)));

        assertThat(docs).hasSize(2);
        assertThat(docs.get(0).getText()).contains("Paragraph 1");
        assertThat(docs.get(1).getText()).contains("Paragraph 2");
    }

    @Test
    void shouldFallbackToTokenSplitter() {
        String longText = "# Long Section\n" + "A ".repeat(200);
        List<Document> docs = splitter.apply(List.of(new Document(longText)));

        assertThat(docs.size()).isGreaterThan(1);
        assertThat(docs.get(0).getMetadata()).containsEntry("section_title", "# Long Section");
        assertThat(docs.get(1).getMetadata()).containsEntry("section_title", "# Long Section");
    }
}
