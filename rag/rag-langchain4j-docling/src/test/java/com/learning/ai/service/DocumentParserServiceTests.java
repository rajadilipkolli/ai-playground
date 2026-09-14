package com.learning.ai.service;

import static org.assertj.core.api.Assertions.assertThat;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class DocumentParserServiceTests {

    @Test
    void fallbackReceivesFreshCopyOfInput() {
        byte[] input = "complete document".getBytes(StandardCharsets.UTF_8);
        DocumentParser primary = stream -> {
            readAllBytes(stream);
            throw new IllegalStateException("primary failed");
        };
        AtomicReference<byte[]> fallbackInput = new AtomicReference<>();
        DocumentParser fallback = stream -> {
            fallbackInput.set(readAllBytes(stream));
            return Document.from("fallback");
        };
        DocumentParserService service = new DocumentParserService(primary, fallback);

        Document result = service.parse(new ByteArrayInputStream(input));

        assertThat(result.text()).isEqualTo("fallback");
        assertThat(fallbackInput.get()).isEqualTo(input);
    }

    private static byte[] readAllBytes(java.io.InputStream stream) {
        try {
            return stream.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
