package com.learning.ai.service;

import static org.assertj.core.api.Assertions.assertThat;

import dev.langchain4j.data.segment.TextSegment;
import java.util.List;
import org.junit.jupiter.api.Test;

class MetadataEnricherTests {

    /** Verifies that sibling headings replace the prior heading at their level. */
    @Test
    void replacesSiblingHeadingsAtTheSameLevel() {
        List<TextSegment> segments = List.of(
                TextSegment.from("# Parent"),
                TextSegment.from("## First child"),
                TextSegment.from("text"),
                TextSegment.from("## Second child"));

        new MetadataEnricher().enrich(segments, "file.pdf", "/allowed/file.pdf", "hash");

        assertThat(segments.get(0).metadata().getString("section_path")).isEqualTo("root/Parent");
        assertThat(segments.get(1).metadata().getString("section_path")).isEqualTo("root/Parent/Firstchild");
        assertThat(segments.get(2).metadata().getString("section_path")).isEqualTo("root/Parent/Firstchild");
        assertThat(segments.get(3).metadata().getString("section_path")).isEqualTo("root/Parent/Secondchild");
        assertThat(segments.get(3).metadata().getString("source_path")).isEqualTo("/allowed/file.pdf");
    }
}
